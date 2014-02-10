package ca.ubc.cs.beta.aclib.misc.file;

import java.io.File;

public class HomeFileUtils {
	public static File getHomeFile(String filename)
	{
		return new File(System.getProperty("user.home") + File.separator + filename);
	}
}
