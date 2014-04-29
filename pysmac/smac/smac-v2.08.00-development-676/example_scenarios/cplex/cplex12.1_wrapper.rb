#!/usr/bin/ruby

# Deal with inputs.
if ARGV.length < 11 
    puts "cplex12.1_wrapper.rb is a wrapper for CPLEX"
    puts "Usage: ruby cplex12.1_wrapper.rb --memoryLimit <memLimit in MB> --runsolverLocation <directory with runsolver> --allowedMIPgap <allowed MIP gap, e.g. 0.0001> <instance_relname> <instance_specifics (string in \"quotes\"> <cutoff_time> <cutoff_length> <seed> <params to be passed on as -param value pairs>."
    exit -1
end

memoryLimit = nil; # MiB
runsolverLocation = nil;
spearLocation = nil;

ARGV[0..5].each_index { |i|
        if (i % 2 == 0)
                if (ARGV[i] == "--memoryLimit")
                        memoryLimit = ARGV[i+1].to_i
                elsif (ARGV[i] == "--runsolverLocation")
                        runsolverLocation = ARGV[i+1]
                elsif (ARGV[i] == "--allowedMIPgap")
                        $allowed_mipgap = ARGV[i+1].to_f
                end
        end
}

$instance = ARGV[6]
$instance_specifics = ARGV[7]
$runtimeLimit = ARGV[8].to_f
$runlengthLimit = ARGV[9].to_i
$seed = ARGV[10]

DELAY_BEFORE_SIGKILL=2
GRACEPERIOD_MULTIPLIER=1.1
GRACEPERIOD_ADDITIVE=1

################################################
# Parse standard inputs, and CPLEX parameters
################################################
argv = ARGV[6...ARGV.length]
$instance = argv[0]
$instance_specifics = argv[1] #=== Here instance_specifics could be used to verify the result CPLEX computes.
$runtimeLimit = argv[2].to_f
$runlengthLimit = argv[3].to_i
$seed = argv[4]

cmd = "ruby ./run_cplex12.1.rb #{$instance} #{$runtimeLimit} #{memoryLimit}"
i = 5
if argv[5] == "-param_string" and not argv[6] == "default-params"
	i=6 # this helps to specify local modifications of the default as returned by the CPLEX tuning tool.
end
unless argv[5] == "-param_string" and argv[6] == "default-params" 
	simplex_perturbation_switch = "no"
	perturbation_constant = "1e-6"
	while i <= argv.length-2
		param = argv[i].sub(/^-/,"")
		
		case param
			#=== Deal with 2 parameters in one in "simplex perturbation"
			when "simplex_perturbation_switch"
				simplex_perturbation_switch = argv[i+1]
			when "perturbation_constant"
				perturbation_constant = argv[i+1]
#				p "updating perturbation_constant to #{perturbation_constant }"
			
			#=== Deal with relative time solution for solution polishing
			when "mip_polishafter_time_rel"
				relative_time_percent = argv[i+1].to_f
				absolute_time = (timeout+0.1) * relative_time_percent / 100
				cmd << " mip_polishafter_time=#{absolute_time}"
			
			#== Deal with bbinterval: only allow 1+
			when "mip_strategy_bbinterval"
				bbinterval = argv[i+1].to_i
				bbinterval = 1000 if bbinterval == 0
				cmd << " mip_strategy_bbinterval=#{bbinterval}"

			else			
				cmd << " #{param}=#{argv[i+1]}"
		end
		i += 2
	end
	#===  Very annoying, two parameters in one! No auto. Binary & R+ (>=1e-8) for the two params. This is also not discussed in the parameters reference manual.
	cmd << " 'simplex_perturbation=#{simplex_perturbation_switch} #{perturbation_constant}'"
end

################################################
# Set parameters to pass to runcplex
################################################
$memoryCutoff = memoryLimit * 1024 * 1024 # convert MB to bytes
graceRuntimeLimit = $runtimeLimit*GRACEPERIOD_MULTIPLIER + GRACEPERIOD_ADDITIVE
$path_to_runcplex_script = "."
tmpdir = "."

$cmdArray = [runsolverLocation, "-C", "#{graceRuntimeLimit}", "-M", "#{memoryLimit}", "-d", "#{DELAY_BEFORE_SIGKILL}", cmd]
