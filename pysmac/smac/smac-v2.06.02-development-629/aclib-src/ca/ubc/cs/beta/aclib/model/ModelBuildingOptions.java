package ca.ubc.cs.beta.aclib.model;

import ca.ubc.cs.beta.aclib.misc.jcommander.validator.NonNegativeInteger;
import ca.ubc.cs.beta.aclib.misc.options.OptionLevel;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;

import com.beust.jcommander.Parameter;

@UsageTextField(hiddenSection = true)
public class ModelBuildingOptions extends AbstractOptions{

	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--mask-censored-data-as-kappa-max","--maskCensoredDataAsKappaMax"}, description="Mask censored data as kappa Max")
	public boolean maskCensoredDataAsKappaMax = false;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--mask-inactive-conditional-parameters-as-default-value","--maskInactiveConditionalParametersAsDefaultValue"}, description="build the model treating inactive conditional values as the default value")
	public boolean maskInactiveConditionalParametersAsDefaultValue = true;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--treat-censored-data-as-uncensored","--treatCensoredDataAsUncensored"}, description="builds the model as-if the response values observed for cap values, were the correct ones [NOT RECOMMENDED]")
	public boolean maskCensoredDataAsUncensored = false;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--imputation-iterations","--imputationIterations"}, description="amount of times to impute censored data when building model", validateWith=NonNegativeInteger.class)
	public int imputationIterations = 2;

	
}
