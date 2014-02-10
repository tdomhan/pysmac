package ca.ubc.cs.beta.aclib.misc.options;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@Retention(RetentionPolicy.RUNTIME)
public @interface UsageTextField {
	
	String titlebanner() default "========== %-20s ==========%n%n";
	
	String description() default "" ;
	
	String defaultValues()  default "<NOT SET>";

	String title() default "";

	String domain() default "<NOT SET>";

	boolean hiddenSection() default false;
	
	String[] claimRequired() default {};
	
	Class<? extends NoArgumentHandler> noarg() default NoopNoArgumentHandler.class;
	
	Class<? extends Object> converterFileOptions() default Object.class;
	
	OptionLevel level() default OptionLevel.BASIC;
}
