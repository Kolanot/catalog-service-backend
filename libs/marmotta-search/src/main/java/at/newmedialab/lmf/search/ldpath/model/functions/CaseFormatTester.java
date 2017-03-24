package at.newmedialab.lmf.search.ldpath.model.functions;

import com.google.common.base.CaseFormat;

public class CaseFormatTester {
	public static void main(String[]args) {
		new CaseFormatTester();
	}

	public CaseFormatTester() {
		// TODO Auto-generated constructor stub
		
		String a = "Responsible Person     (DEMO)";
		String b = "Weight";
		System.out.println(CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, preFormat(a)));
		System.out.println(CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, preFormat(b)));
	}
	private String preFormat(String in) {
		in = in.replaceAll("[^a-zA-Z0-9 ]", "");
		return in.trim().replaceAll(" ", "_").toUpperCase();
		
	}

}
