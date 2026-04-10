package com.dermaCare.customerService.util;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OtpUtil {
    public static String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }
    
    public static boolean isEmail(String str) {
		String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
		Pattern pattern = Pattern.compile(emailRegex);
		Matcher matcher = pattern.matcher(str);
		return matcher.matches();
	}
	   public static boolean isMobileNumber(String str) {
	       // Simple mobile number pattern (adjust as needed)
	       String mobileRegex = "^\\d{10}$"; // Assumes 10-digit mobile number
	       Pattern pattern = Pattern.compile(mobileRegex);
	       Matcher matcher = pattern.matcher(str);
	       return matcher.matches();
	   }
}
