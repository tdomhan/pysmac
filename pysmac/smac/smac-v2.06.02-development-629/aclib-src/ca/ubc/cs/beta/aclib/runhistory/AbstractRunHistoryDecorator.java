package ca.ubc.cs.beta.aclib.runhistory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.exceptions.DuplicateRunException;
import ca.ubc.cs.beta.aclib.objectives.OverallObjective;
import ca.ubc.cs.beta.aclib.objectives.RunObjective;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceSeedPair;


/**
 * Abstract class that delegates all methods to the supplied runhistory object.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public abstract class AbstractRunHistoryDecorator implements RunHistory {

	
	protected final RunHistory rh;
	
	public AbstractRunHistoryDecorator(RunHistory rh)
	{
		this.rh = rh;
	}

	@Override
	public void append(AlgorithmRun run) throws DuplicateRunException {
		rh.append(run);
	}

	@Override
	public RunObjective getRunObjective() {
		return rh.getRunObjective();
	}

	@Override
	public OverallObjective getOverallObjective() {
		return rh.getOverallObjective();
	}

	@Override
	public void incrementIteration() {
		rh.incrementIteration();
	}

	@Override
	public int getIteration() {
		return rh.getIteration();
	}

	@Override
	public Set<ProblemInstance> getInstancesRan(ParamConfiguration config) {
		return rh.getInstancesRan(config);
	}

	@Override
	public Set<ProblemInstanceSeedPair> getAlgorithmInstanceSeedPairsRan(
			ParamConfiguration config) {
		return rh.getAlgorithmInstanceSeedPairsRan(config);
	}

	@Override
	public double getEmpiricalCost(ParamConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime) {
		return rh.getEmpiricalCost(config, instanceSet, cutoffTime);
	}

	@Override
	public double getEmpiricalCost(ParamConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime,
			Map<ProblemInstance, Map<Long, Double>> hallucinatedValues) {
		return rh.getEmpiricalCost(config, instanceSet, cutoffTime,
				hallucinatedValues);
	}

	@Override
	public double getEmpiricalCost(ParamConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime,
			Map<ProblemInstance, Map<Long, Double>> hallucinatedValues,
			double minimumResponseValue) {
		return rh.getEmpiricalCost(config, instanceSet, cutoffTime,
				hallucinatedValues, minimumResponseValue);
	}

	@Override
	public int getTotalNumRunsOfConfig(ParamConfiguration config) {
		return rh.getTotalNumRunsOfConfig(config);
	}

	@Override
	public double getTotalRunCost() {
		return rh.getTotalRunCost();
	}

	@Override
	public double[] getRunResponseValues() {
		return rh.getRunResponseValues();
	}

	@Override
	public Set<ProblemInstance> getUniqueInstancesRan() {
		return rh.getUniqueInstancesRan();
	}

	@Override
	public Set<ParamConfiguration> getUniqueParamConfigurations() {
		return rh.getUniqueParamConfigurations();
	}

	@Override
	public int[][] getParameterConfigurationInstancesRanByIndex() {
		return rh.getParameterConfigurationInstancesRanByIndex();
	}

	@Override
	public boolean[] getCensoredFlagForRuns() {
		return rh.getCensoredFlagForRuns();
	}

	@Override
	public List<ParamConfiguration> getAllParameterConfigurationsRan() {
		return rh.getAllParameterConfigurationsRan();
	}

	@Override
	public double[][] getAllConfigurationsRanInValueArrayForm() {
		return rh.getAllConfigurationsRanInValueArrayForm();
	}

	@Override
	public List<AlgorithmRun> getAlgorithmRuns() {
		return rh.getAlgorithmRuns();
	}

	@Override
	public List<RunData> getAlgorithmRunData() {
		return rh.getAlgorithmRunData();
	}

	@Override
	public List<AlgorithmRun> getAlgorithmRunData(ParamConfiguration config) {
		return rh.getAlgorithmRunData(config);
	}

	@Override
	public Set<ProblemInstanceSeedPair> getCappedAlgorithmInstanceSeedPairs(
			ParamConfiguration config) {
		return rh.getCappedAlgorithmInstanceSeedPairs(config);
	}

	@Override
	public int getThetaIdx(ParamConfiguration configuration) {
		return rh.getThetaIdx(configuration);
	}

	@Override
	public double getEmpiricalCost(ParamConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime,
			double minimumResponseValue) {
		return rh.getEmpiricalCost(config, instanceSet, cutoffTime,
				minimumResponseValue);
	}

	@Override
	public int getNumberOfUniqueProblemInstanceSeedPairsForConfiguration(
			ParamConfiguration config) {
		return rh
				.getNumberOfUniqueProblemInstanceSeedPairsForConfiguration(config);
	}

	@Override
	public Map<ProblemInstance, LinkedHashMap<Long, Double>> getPerformanceForConfig(
			ParamConfiguration configuration) {
		return rh.getPerformanceForConfig(configuration);
	}

	@Override
	public List<Long> getSeedsUsedByInstance(ProblemInstance pi) {
		return rh.getSeedsUsedByInstance(pi);
	}

	
}
