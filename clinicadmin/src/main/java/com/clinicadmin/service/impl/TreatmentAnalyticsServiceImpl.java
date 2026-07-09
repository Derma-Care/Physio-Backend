package com.clinicadmin.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.Session;
import com.clinicadmin.dto.physioresponse.ExerciseResponse;
import com.clinicadmin.dto.physioresponse.PackageResponse;
import com.clinicadmin.dto.physioresponse.PaymentRecordResponse;
import com.clinicadmin.dto.physioresponse.ProgramResponse;
import com.clinicadmin.dto.physioresponse.TherapyResponse;
import com.clinicadmin.dto.physioresponse.TreatmentAnalyticsResponse;
import com.clinicadmin.dto.physioresponse.TreatmentRow;
import com.clinicadmin.feignclient.PhysiotherapyFeignClient;
import com.clinicadmin.service.TreatmentAnalyticsService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TreatmentAnalyticsServiceImpl implements TreatmentAnalyticsService {

	@Autowired
	private PhysiotherapyFeignClient physiotherapyDoctorFeign;

	private final ObjectMapper mapper = new ObjectMapper();

	// one flattened "treatment occurrence" before grouping
	private static class Entry {
		String name;
		String type; // Activity / Therapy / Program / Package
		String patientId;
		int sessions;
		int completed;
		double revenue;
	}

	// ========================================================
	// PUBLIC: PERIOD BASED (Today / Month / Quarter / Year)
	// ========================================================
	@Override
	public Response getTreatmentAnalytics(String clinicId, String branchId, String type, String period) {
		Predicate<LocalDate> dateFilter = buildPeriodFilter(period);
		TreatmentAnalyticsResponse analytics = getAnalytics(clinicId, branchId, type, dateFilter);
		return Response.builder().success(true).status(200).message("Treatment analytics fetched successfully")
				.data(analytics).build();
	}

	// ========================================================
	// PUBLIC: CUSTOM DATE RANGE (inclusive, yyyy-MM-dd)
	// ========================================================
	@Override
	public Response getTreatmentAnalyticsByDateRange(String clinicId, String branchId, String type, String fromDate,
			String toDate) {

		LocalDate from = LocalDate.parse(fromDate);
		LocalDate to = LocalDate.parse(toDate);

		if (from.isAfter(to)) {
			LocalDate tmp = from;
			from = to;
			to = tmp;
		}

		final LocalDate f = from;
		final LocalDate t = to;
		Predicate<LocalDate> dateFilter = d -> !d.isBefore(f) && !d.isAfter(t);

		TreatmentAnalyticsResponse analytics = getAnalytics(clinicId, branchId, type, dateFilter);

		return Response.builder().success(true).status(200).message("Treatment analytics fetched successfully")
				.data(analytics).build();
	}

	// ========================================================
	// SHARED CORE
	// ========================================================
	private TreatmentAnalyticsResponse getAnalytics(String clinicId, String branchId, String type,
			Predicate<LocalDate> dateFilter) {

		List<PaymentRecordResponse> records = fetchPaymentRecords(clinicId, branchId);
		List<Entry> entries = new ArrayList<>();

		for (PaymentRecordResponse record : records) {
			if (record.getServiceType() == null)
				continue;

			String serviceType = record.getServiceType().toLowerCase();
			String category = toCategory(serviceType);

			// "type" filter: exercise=Activity, therapy=Therapy, program=Program,
			// package=Package
			if (type != null && !type.equalsIgnoreCase("All Types") && !category.equalsIgnoreCase(type)) {
				continue;
			}

			entries.addAll(extractEntries(record, serviceType, category, dateFilter));
		}

		return buildResponse(entries);
	}

	// ========================================================
	// FETCH RECORDS FROM PHYSIOTHERAPY-DOCTOR SERVICE
	// ========================================================
	@SuppressWarnings("unchecked")
	private List<PaymentRecordResponse> fetchPaymentRecords(String clinicId, String branchId) {
		try {
			Response response = physiotherapyDoctorFeign.getPayments(clinicId, branchId);
			if (response == null || response.getData() == null)
				return List.of();

			List<Map<String, Object>> raw = mapper.convertValue(response.getData(), List.class);
			List<PaymentRecordResponse> result = new ArrayList<>();
			for (Map<String, Object> m : raw) {
				result.add(mapper.convertValue(m, PaymentRecordResponse.class));
			}
			return result;
		} catch (Exception e) {
			return List.of();
		}
	}

	private String toCategory(String serviceType) {
		return switch (serviceType) {
		case "exercise" -> "Activity";
		case "therapy" -> "Therapy";
		case "program" -> "Program";
		default -> "Package";
		};
	}

	// ========================================================
	// EXTRACT TOP-LEVEL ENTRIES PER RECORD (per its serviceType)
	// ========================================================
	@SuppressWarnings("unchecked")
	private List<Entry> extractEntries(PaymentRecordResponse record, String serviceType, String category,
			Predicate<LocalDate> dateFilter) {

		List<Entry> list = new ArrayList<>();
		Object data = record.getTherapyWithSessions();
		if (data == null)
			return list;

		List<Map<String, Object>> rawList = mapper.convertValue(data, List.class);

		switch (serviceType) {
		case "exercise" -> {
			for (Map<String, Object> m : rawList) {
				ExerciseResponse ex = mapper.convertValue(m, ExerciseResponse.class);
				list.add(fromExercise(ex, category, record.getPatientId(), dateFilter));
			}
		}
		case "therapy" -> {
			for (Map<String, Object> m : rawList) {
				TherapyResponse t = mapper.convertValue(m, TherapyResponse.class);
				list.add(fromTherapy(t, category, record.getPatientId(), dateFilter));
			}
		}
		case "program" -> {
			for (Map<String, Object> m : rawList) {
				ProgramResponse p = mapper.convertValue(m, ProgramResponse.class);
				list.add(fromProgram(p, category, record.getPatientId(), dateFilter));
			}
		}
		default -> { // package
			for (Map<String, Object> m : rawList) {
				PackageResponse pkg = mapper.convertValue(m, PackageResponse.class);
				list.add(fromPackage(pkg, category, record.getPatientId(), dateFilter));
			}
		}
		}
		return list;
	}

	private Entry fromExercise(ExerciseResponse ex, String category, String patientId,
			Predicate<LocalDate> dateFilter) {
		Entry e = new Entry();
		e.name = ex.getExerciseName();
		e.type = category;
		e.patientId = patientId;
		int[] c = countSessions(ex.getSessions(), dateFilter);
		e.sessions = c[0];
		e.completed = c[1];
		e.revenue = ex.getTotalExercisePrice() != null ? ex.getTotalExercisePrice() : ex.getTotalPrice();
		return e;
	}

	private Entry fromTherapy(TherapyResponse t, String category, String patientId, Predicate<LocalDate> dateFilter) {
		Entry e = new Entry();
		e.name = t.getTherapyName();
		e.type = category;
		e.patientId = patientId;
		int sessions = 0, completed = 0;
		if (t.getExercises() != null) {
			for (ExerciseResponse ex : t.getExercises()) {
				int[] c = countSessions(ex.getSessions(), dateFilter);
				sessions += c[0];
				completed += c[1];
			}
		}
		e.sessions = sessions;
		e.completed = completed;
		e.revenue = t.getTotalTherapyPrice() != null ? t.getTotalTherapyPrice() : 0;
		return e;
	}

	private Entry fromProgram(ProgramResponse p, String category, String patientId, Predicate<LocalDate> dateFilter) {
		Entry e = new Entry();
		e.name = p.getProgramName();
		e.type = category;
		e.patientId = patientId;
		int sessions = 0, completed = 0;
		if (p.getTherapyData() != null) {
			for (TherapyResponse t : p.getTherapyData()) {
				if (t.getExercises() == null)
					continue;
				for (ExerciseResponse ex : t.getExercises()) {
					int[] c = countSessions(ex.getSessions(), dateFilter);
					sessions += c[0];
					completed += c[1];
				}
			}
		}
		e.sessions = sessions;
		e.completed = completed;
		e.revenue = p.getTotalProgramPrice() != null ? p.getTotalProgramPrice() : 0;
		return e;
	}

	private Entry fromPackage(PackageResponse pkg, String category, String patientId, Predicate<LocalDate> dateFilter) {
		Entry e = new Entry();
		e.name = pkg.getPackageName();
		e.type = category;
		e.patientId = patientId;
		int sessions = 0, completed = 0;
		if (pkg.getPrograms() != null) {
			for (ProgramResponse p : pkg.getPrograms()) {
				if (p.getTherapyData() == null)
					continue;
				for (TherapyResponse t : p.getTherapyData()) {
					if (t.getExercises() == null)
						continue;
					for (ExerciseResponse ex : t.getExercises()) {
						int[] c = countSessions(ex.getSessions(), dateFilter);
						sessions += c[0];
						completed += c[1];
					}
				}
			}
		}
		e.sessions = sessions;
		e.completed = completed;
		e.revenue = pkg.getTotalPackagePrice() != null ? pkg.getTotalPackagePrice() : 0;
		return e;
	}

	// ========================================================
	// SESSION COUNTING WITH DATE FILTER -> {total, completed}
	// ========================================================
	private int[] countSessions(List<Session> sessions, Predicate<LocalDate> dateFilter) {
		int total = 0, completed = 0;
		if (sessions == null)
			return new int[] { 0, 0 };

		for (Session s : sessions) {
			if (s.getDate() == null)
				continue;
			LocalDate d;
			try {
				d = LocalDate.parse(s.getDate());
			} catch (Exception ex) {
				continue;
			}
			if (!dateFilter.test(d))
				continue;

			total++;
			if ("Completed".equalsIgnoreCase(s.getStatus()))
				completed++;
		}
		return new int[] { total, completed };
	}

	// ========================================================
	// PERIOD -> PREDICATE
	// ========================================================
	private Predicate<LocalDate> buildPeriodFilter(String period) {
		LocalDate today = LocalDate.now();

		if (period == null)
			return d -> true;

		return switch (period.toLowerCase()) {
		case "today" -> d -> d.isEqual(today);
		case "month" -> d -> d.getMonth() == today.getMonth() && d.getYear() == today.getYear();
		case "quarter" ->
			d -> ((d.getMonthValue() - 1) / 3) == ((today.getMonthValue() - 1) / 3) && d.getYear() == today.getYear();
		case "year" -> d -> d.getYear() == today.getYear();
		default -> d -> true;
		};
	}

	// ========================================================
	// GROUP ENTRIES BY (name, type) -> BUILD FINAL RESPONSE
	// ========================================================
	private TreatmentAnalyticsResponse buildResponse(List<Entry> entries) {

		Map<String, TreatmentRow> grouped = new LinkedHashMap<>();
		Map<String, Set<String>> patientsByTreatment = new LinkedHashMap<>();
		Map<String, List<Double>> revenueByTreatment = new LinkedHashMap<>();

		for (Entry e : entries) {
			// no sessions/revenue in the selected window -> exclude, so the filter
			// actually narrows the table instead of just zeroing counts
			if (e.sessions == 0 && e.completed == 0 && e.revenue == 0)
				continue;

			String key = e.name + "|" + e.type;
			TreatmentRow row = grouped.computeIfAbsent(key, k -> {
				TreatmentRow r = new TreatmentRow();
				r.setTreatmentName(e.name);
				r.setType(e.type);
				return r;
			});
			row.setSessions(row.getSessions() + e.sessions);
			row.setCompleted(row.getCompleted() + e.completed);

			patientsByTreatment.computeIfAbsent(key, k -> new HashSet<>()).add(e.patientId);
			revenueByTreatment.computeIfAbsent(key, k -> new ArrayList<>()).add(e.revenue);
		}

		for (Map.Entry<String, TreatmentRow> en : grouped.entrySet()) {
			String key = en.getKey();
			TreatmentRow row = en.getValue();
			row.setPatients(patientsByTreatment.getOrDefault(key, Set.of()).size());
			row.setSuccessRate(row.getSessions() > 0 ? round2(row.getCompleted() * 100.0 / row.getSessions()) : 0);
			List<Double> revs = revenueByTreatment.getOrDefault(key, List.of());
			double avgRev = revs.isEmpty() ? 0 : revs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
			row.setAvgRevenue(round2(avgRev));
		}

		List<TreatmentRow> rows = new ArrayList<>(grouped.values());

		TreatmentAnalyticsResponse res = new TreatmentAnalyticsResponse();
		res.setTreatments(rows);
		res.setTotalTreatmentTypes(rows.size());
		res.setTotalTreatments(rows.size());
		res.setTotalSessions(rows.stream().mapToInt(TreatmentRow::getSessions).sum());

		Set<String> allPatients = new HashSet<>();
		for (Set<String> s : patientsByTreatment.values())
			allPatients.addAll(s);
		res.setTotalPatients(allPatients.size());

		res.setAvgSuccessRate(rows.isEmpty() ? 0
				: round2(rows.stream().mapToDouble(TreatmentRow::getSuccessRate).average().orElse(0)));

		res.setHighlyRatedCount((int) rows.stream().filter(r -> r.getSuccessRate() >= 90).count());

		res.setAvgRevenuePerTreatment(rows.isEmpty() ? 0
				: round2(rows.stream().mapToDouble(TreatmentRow::getAvgRevenue).average().orElse(0)));

		return res;
	}

	private double round2(double v) {
		return Math.round(v * 100.0) / 100.0;
	}
}