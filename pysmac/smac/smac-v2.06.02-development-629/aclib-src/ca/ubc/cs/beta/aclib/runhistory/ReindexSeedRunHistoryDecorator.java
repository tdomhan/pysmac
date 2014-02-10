package ca.ubc.cs.beta.aclib.runhistory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jcip.annotations.NotThreadSafe;
import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.ExistingAlgorithmRun;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.exceptions.DuplicateRunException;
import ca.ubc.cs.beta.aclib.misc.MapList;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;


/**
 * Decorator that changes seeds of runs to by there run number
 * this is primarily useful for merging many different state files
 * together and getting there runs for different configurations on the same instance to agree.
 * 
 * This isn't ideal, but it's generally better than having a bunch of runs for the same PI differ by seed.
 * 
 * <br>
 * <b>NOTE:</b> This class is not thread safe 
 *
 *
 *
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@NotThreadSafe
public class ReindexSeedRunHistoryDecorator extends AbstractRunHistoryDecorator implements RunHistory{

	
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Random rand;
	
	public ReindexSeedRunHistoryDecorator(RunHistory rh, Random rand) {
		super(rh);
		this.rand = rand;
	}


	private final Map<ProblemInstanceSeedPair, ProblemInstanceSeedPair> pispTransform = new HashMap<ProblemInstanceSeedPair, ProblemInstanceSeedPair>();

	private final MapList<ProblemInstance, ProblemInstanceSeedPair> mpi = new MapList<ProblemInstance, ProblemInstanceSeedPair>(new HashMap<ProblemInstance, List<ProblemInstanceSeedPair>>());
	
	AtomicInteger nextSeed = new AtomicInteger(0);
	
	AtomicInteger duplicateRunsDropped = new AtomicInteger(0);
	@Override
	public void append(AlgorithmRun run) throws DuplicateRunException {
		
		
		
		ProblemInstanceSeedPair pisp = run.getRunConfig().getProblemInstanceSeedPair();
		
		if(run.getRunConfig().getParamConfiguration().getConfigurationSpace().getDefaultConfiguration().equals(run.getRunConfig().getParamConfiguration()))
		{
			log.debug("Transforming run of default configuration {}", run);
		}
		
		
		
		if(pispTransform.get(pisp) != null)
		{
			RunConfig newRc = new RunConfig(pispTransform.get(pisp), run.getRunConfig().getCutoffTime(), run.getRunConfig().getParamConfiguration(), run.getRunConfig().hasCutoffLessThanMax());
			
			ExistingAlgorithmRun er = new ExistingAlgorithmRun(run.getExecutionConfig(), newRc, run.getRunResult(), run.getRuntime(), run.getRunLength(), run.getQuality(),pispTransform.get(pisp).getSeed(), run.getAdditionalRunData(), run.getWallclockExecutionTime());

			try { 
				rh.append(er);
			return;
			} catch(DuplicateRunException e)
			{
				log.debug("Duplicate run has been dropped, so far: {} ",  duplicateRunsDropped.incrementAndGet());
			}
		} else
		{
		
			
			List<ProblemInstanceSeedPair> possiblePisps = new ArrayList<ProblemInstanceSeedPair>(mpi.getList(pisp.getInstance()));
			Collections.shuffle(possiblePisps, rand);
			for(ProblemInstanceSeedPair newPisp : possiblePisps)
			{
				RunConfig newRc = new RunConfig(newPisp, run.getRunConfig().getCutoffTime(), run.getRunConfig().getParamConfiguration(), run.getRunConfig().hasCutoffLessThanMax());
				
				ExistingAlgorithmRun er = new ExistingAlgorithmRun(run.getExecutionConfig(), newRc, run.getRunResult(), run.getRuntime(), run.getRunLength(), run.getQuality(),newPisp.getSeed(), run.getAdditionalRunData(), run.getWallclockExecutionTime());

				try { 
					rh.append(er);
					pispTransform.put(pisp, newPisp);
					return;
				} catch(DuplicateRunException e)
				{
					
				}
			}
			
			
			
			ProblemInstanceSeedPair newPisp = new ProblemInstanceSeedPair(pisp.getInstance(), nextSeed.incrementAndGet()); 
			
			
			RunConfig newRc = new RunConfig(newPisp, run.getRunConfig().getCutoffTime(), run.getRunConfig().getParamConfiguration(), run.getRunConfig().hasCutoffLessThanMax());
			
			ExistingAlgorithmRun er = new ExistingAlgorithmRun(run.getExecutionConfig(), newRc, run.getRunResult(), run.getRuntime(), run.getRunLength(), run.getQuality(),newPisp.getSeed(), run.getAdditionalRunData(), run.getWallclockExecutionTime());

			try { 
				rh.append(er);
				pispTransform.put(pisp, newPisp);
				return;
			} catch(DuplicateRunException e)
			{
				log.debug("Duplicate run has been dropped, so far: {} ",  duplicateRunsDropped.incrementAndGet());
			}
			
		
		
		
		}
		
		
		
		
		
	
	
	}
	@Override
	public int getOrCreateThetaIdx(ParamConfiguration initialIncumbent) {
		return this.rh.getOrCreateThetaIdx(initialIncumbent);
	}
	
}
