package com.dermacare.doctorservice.dermacaredoctorutils;

public class VisitTypeUtil {

    private static final String[] SMALL = {
        "", "FIRST", "SECOND", "THIRD", "FOURTH", "FIFTH",
        "SIXTH", "SEVENTH", "EIGHTH", "NINTH", "TENTH",
        "ELEVENTH", "TWELFTH", "THIRTEENTH", "FOURTEENTH",
        "FIFTEENTH", "SIXTEENTH", "SEVENTEENTH", "EIGHTEENTH",
        "NINETEENTH"
    };
    private static final String[] TENS = {
        "", "", "TWENTIETH", "THIRTIETH", "FORTIETH",
        "FIFTIETH", "SIXTIETH", "SEVENTIETH",
        "EIGHTIETH", "NINETIETH"
    };

    public static String getVisitTypeFromCount(int count) {
        return numberToOrdinal(count) + "_VISIT";
    }

    private static String numberToOrdinal(int num) {
        if (num <= 0) return String.valueOf(num);

        if (num < 20) return SMALL[num];

        int tens = num / 10;
        int ones = num % 10;

        if (ones == 0) return TENS[tens];

        String base;
        switch (tens) {
            case 2: base = "TWENTY"; break;
            case 3: base = "THIRTY"; break;
            case 4: base = "FORTY"; break;
            case 5: base = "FIFTY"; break;
            case 6: base = "SIXTY"; break;
            case 7: base = "SEVENTY"; break;
            case 8: base = "EIGHTY"; break;
            case 9: base = "NINETY"; break;
            default: base = String.valueOf(num);
        }

        return base + "-" + SMALL[ones];
    }
}
