package ca.ubc.cs.beta.aclib.model.builder;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import ca.ubc.cs.beta.aclib.exceptions.TrajectoryDivergenceException;
import ca.ubc.cs.beta.aclib.model.data.SanitizedModelData;
import ca.ubc.cs.beta.aclib.options.RandomForestOptions;
import ca.ubc.cs.beta.aclib.runhistory.RunHistory;
/**
 * Computes a Hash Code of the Built Model
 * 
 * This is primarily used for Matlab Synchronization
 * Feel free to remove this class, as it probably isn't of much help
 * 
 * @author seramage
 *
 */
public class HashCodeVerifyingModelBuilder extends BasicModelBuilder {

	//TODO REMOVE THIS AWFULNESS
	public static Queue<Integer> modelHashes = new LinkedList<Integer>();
	public static Queue<Integer> preprocessedHashes = new LinkedList<Integer>();
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final Marker runHash = MarkerFactory.getMarker("RUN_HASH");
	
	
	public HashCodeVerifyingModelBuilder(SanitizedModelData mds,
			RandomForestOptions rfConfig, RunHistory runHistory, Random rand) {
		super(mds, rfConfig, rand);
		
		int forestCode = forest.matlabHashCode();
		log.info("Random Forest Built with Hash Code: {}", forestCode);
		
		
		if(!modelHashes.isEmpty())
		{
			int expected = modelHashes.poll();
			if(forestCode != expected)
			{
				throw new TrajectoryDivergenceException("Expected Random Forest To Be Built With Hash Code: "+expected+ " vs. " + forestCode);
			} else
			{
				log.info("Random Forest Hash Code Matched");
			}
		}
		
		if(preprocessedForest != null)
		{
			int preprocessedCode = preprocessedForest.matlabHashCode();
			log.info(runHash,"Preprocessed Forest Built with Hash Code: {}",preprocessedCode);
			
			if(!preprocessedHashes.isEmpty())
			{
				int expected = preprocessedHashes.poll();
				if(preprocessedCode != expected)
				{
					throw new TrajectoryDivergenceException("Expected Preprocessed Random Forest To Be Built With Hash Code: "+expected+ " vs. " + preprocessedCode);
				} else
				{
					log.info("Preprocessed Hash Code Matched");
				}

			}
		}
		
		
		
		
		
	}

}
