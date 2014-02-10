package ca.ubc.cs.beta.aclib.misc.version;

import org.mangosdk.spi.ProviderFor;

@ProviderFor(VersionInfo.class)
public class ACLibVersionInfo extends AbstractVersionInfo {

	public ACLibVersionInfo()
	{
		super("Automatic Configurator Library", "aclib-version.txt",true);
	}
}
