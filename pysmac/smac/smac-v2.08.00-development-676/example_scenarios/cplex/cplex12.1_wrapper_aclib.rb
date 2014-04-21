#!/usr/bin/ruby
$DISABLE_ACTUAL_RUN = true
load  "cplex12.1_wrapper.rb"

def float_regexp()
        return '[+-]?\d+(?:\.\d+)?(?:[eE][+-]\d+)?';
end

def get_instance_specifics(input_file)
    #Not checking instance specifics in this version
        return 0
end

################################################# 
# Run CPLEX.
################################################# 

slack_in_my_assertions = 1.0001 # small multiplicative slack in assertions
tmp_file = "tmp_cplex_output#{rand}.txt"
cmd = $cmdArray.join(" ")
exec_cmd = "#{cmd} > #{tmp_file}"

################################################# 
# Parse its output.
################################################# 

#===Ignore passed $instance_specifics; rather check instance specifics myself.
$instance_specifics = get_instance_specifics($instance)

gap = 1e100
obj = 1e100
solved = "CRASHED"
runtime =  ARGV[8].to_f
walltime = nil
    
Signal.trap("TERM") {
	#=== Respond to termination by deleting temporary file and crashing.
	begin
		puts "Result for ParamILS: CRASHED, 0, 0, 0, #{$seed}"
		File.delete(tmp_file)
	ensure
		Process.exit 1
	end
}

begin
	STDOUT.puts "Calling: #{exec_cmd}"
	system exec_cmd

	#=== Parse algorithm output to extract relevant information for configurator.
	#=== The default result is CRASHED, and certain output changes this to SOLVED (SAT/UNSAT) or TIMEOUT.
	#=== If we match outputs for both SOLVED and TIMEOUT, TIMEOUT takes precedence to allow the developer to detect any problems.
	File.open(tmp_file){|file|
		while line = file.gets
			if line =~ /\(gap = #{float_regexp}, (#{float_regexp})%\)/
				gap = $1.to_f
			end

			if line =~ /Solution time\s*=\s*(#{float_regexp})\s*sec\.\s*Iterations\s*=\s*(\d+)\s*Nodes\s*=\s*(\d+)/
				internal_measured_runtime = $1
				iterations = $2
				measured_runlength = $3
			end

			if line =~ /Solution time =\s*(#{float_regexp}) sec\./
				internal_measured_runtime = $1
				raise "CPLEX reports negative time. I thought this bug was fixed in version 12.1." if internal_measured_runtime.to_f < 0
			end

			if line =~ /MIP\s*-\s*Integer optimal solution:\s*Objective\s*=\s*(#{float_regexp})/
				gap = 0
				obj = $1.to_f
				solved = 'SAT'
			end

			if line =~ /MIP\s*-\s*Integer optimal,\s*tolerance\s*\(#{float_regexp}\/#{float_regexp}\):\s*Objective\s*=\s*(#{float_regexp})/
				obj = $1.to_f
				solved = 'SAT'
			end

			if line =~ /Optimal:\s*Objective =\s*#{float_regexp}/
				solved = 'SAT'
			end
		
			if line =~ /No problem exists./
				solved = 'CRASHED'
			end

			if line =~ /MIP\s*-\s*Time limit exceeded, integer feasible:\s*Objective\s*=\s*(#{float_regexp})/
				obj = $1.to_f
				solved = 'TIMEOUT'
			end

			if line =~ /MIP\s*-\s*Error termination, integer feasible:\s*Objective\s*=\s*(#{float_regexp})/
				obj = $1.to_f
				solved = 'TIMEOUT'
			end

			if line =~ /MIP - Error termination, no integer solution./
				solved = 'TIMEOUT'
			end

			if line =~ /MIP - Time limit exceeded, no integer solution./
				solved = 'TIMEOUT'
			end
			
			if line =~ /CPLEX Error  1001: Out of memory./
				solved = 'TIMEOUT'
			end
			
			if line =~ /MIP - Memory limit exceeded/
				solved = 'TIMEOUT'
			end

			if line =~ /CPLEX Error  3019: Failure to solve MIP subproblem./
				solved = 'TIMEOUT'
			end

			if line =~ /MIP - Time limit exceeded/
				solved = 'TIMEOUT'
				puts "Setting solved=TIMEOUT since we matched: 'MIP - Time limit exceeded'"
			end
			
			if line =~ /Filesize limit exceeded/
				solved = 'TIMEOUT'
			end
			
			if line =~ /Polishing requested, but no solution to polish./
				solved = 'TIMEOUT'
			end
			
			if line =~ /SIGSEGV\s*\(signal\s*11\)/
				puts 'Matched SIGSEGV (signal 11)'
				solved = 'TIMEOUT'
			end

			if line =~ /CPLEX EXIT CODE: 9/ or line =~ /SIGKILL/
				solved = 'TIMEOUT'
			end

#				if line =~ /Infeasible/
#					#raise "CPLEX claims instance to be infeasible"
#					solved = 'WRONG ANSWER'
#				end
#				
#				if line =~/MIP - Integer infeasible./
#					solved = 'WRONG ANSWER'
#				end

			if line =~ /runsolver_max_cpu_time_exceeded/
				solved = "TIMEOUT"
			end
			if line =~ /runsolver_max_memory_limit_exceeded/
				solved = "TIMEOUT"
			end
			if line =~ /runsolver_walltime: (#{float_regexp})$/
				walltime = $1.to_f
			end
			if line =~ /runsolver_cputime: (#{float_regexp})$/
				runtime = $1.to_f
			end
		end
	}
	
	if solved == "SAT"
		raise "solved but no objective value report --- must be a parsing problem" unless obj

		#=== Check correctness.
		unless $instance_specifics.to_f == 0 || $instance_specifics == "instance_specific" # for backwards compatibility with my previous runs
			maxi = [obj.abs, $instance_specifics.to_f.abs].max
			p obj
			p $instance_specifics
			p maxi
			p slack_in_my_assertions
			#p $allowed_mipgap
			if (obj.abs - $instance_specifics.to_f.abs).abs/maxi > slack_in_my_assertions  && (obj.abs - $instance_specifics.to_f.abs).abs > 1e-8
				solved = "WRONG ANSWER" 
				#raise "CPLEX claims to have solved the instance, but its result (#{obj.abs}) differs from the actual one (#{$instance_specifics.to_f.abs}) by more than a relative error of 0.01%."
			end
		end
	end

	if runtime.to_f > $runtimeLimit.to_f
		solved = "TIMEOUT"
	end
	if solved == 'TIMEOUT' && runtime.to_f < $runtimeLimit.to_f 
		measured_runtime = $runtimeLimit.to_f + 0.01
	end

ensure
	################################################# 
	# Output result string.
	################################################# 
	if solved == "TIMEOUT" and runtime==nil # This should NOT be needed since the runsolver output should always contain runsolver_cputime, but at least for Glucose there were TIMEOUT cases where it did not!
        	runtime = $runtimeLimit
    	end
	puts "Result for ParamILS: #{solved}, #{runtime}, #{obj}, #{gap}, #{$seed}" # mis-using the runlength field to store objective reached

	if runtime == nil
		Process.exit 1 # without deleting the tmp_file
	end
	begin
		File.delete(tmp_file)
	rescue Errno::ENOENT # ignore ENOENT errors. (errno 2)
	end

	if solved == "CRASHED"
		Process.exit 1
	end
end
