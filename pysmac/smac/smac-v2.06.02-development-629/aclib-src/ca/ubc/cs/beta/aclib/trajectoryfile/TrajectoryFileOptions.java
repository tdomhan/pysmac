package ca.ubc.cs.beta.aclib.trajectoryfile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aclib.configspace.ParamConfigurationSpace;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;

@UsageTextField(hiddenSection = true)
public class TrajectoryFileOptions extends AbstractOptions{

	@Parameter(names={"--trajectory-file","--trajectoryFile"}, description="Trajectory File to read configurations from")
	public File trajectoryFile;
	
	@Parameter(names={"--trajectory-use-tunertime-if-no-walltime","--useTunerTimeIfNoWallTime"}, description="Use the tuner time as walltime if there is no walltime in the trajectory file")
	public boolean useTunerTimeIfNoWallTime;

	public List<TrajectoryFileEntry> parseTrajectoryFile(ParamConfigurationSpace configSpace) throws FileNotFoundException, IOException {
		return TrajectoryFileParser.parseTrajectoryFileAsList(trajectoryFile, configSpace, useTunerTimeIfNoWallTime);
	}
	
	
	
}
