package ro.binnar.projects.alfa;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class JobLauncherController {

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private JobExplorer jobExplorer;

	@Autowired
	private Job job;

	@RequestMapping(value = "/launchJob", method = RequestMethod.GET)
	@ResponseBody
	public String launchJob(@RequestParam("pathToFile") String pathToFile) throws Exception {
		JobParametersBuilder builder = new JobParametersBuilder();

		builder.addLong("timestamp", System.currentTimeMillis());
		builder.addString("pathToFile", pathToFile);

		JobExecution jobExecution = jobLauncher.run(job, builder.toJobParameters());

		return jobExecution.getId().toString();
	}

	@RequestMapping(value = "/checkJobStatus/{jobExecutionId}", method = RequestMethod.GET)
	@ResponseBody
	public String checkJobStatus(@PathVariable("jobExecutionId") Long jobExecutionId) throws Exception {
		String status = "NOT EXISTS";

		JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);

		if (jobExecution != null) {
			status = jobExecution.getStatus().toString();
		}

		return status;
	}
}