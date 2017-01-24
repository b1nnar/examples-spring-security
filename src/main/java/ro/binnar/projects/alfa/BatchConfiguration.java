package ro.binnar.projects.alfa;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.UrlResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private DataSource dataSource;

	@Bean
	@StepScope
	public FlatFileItemReader<Person> reader(@Value("#{jobParameters[pathToFile]}") String pathToFile) throws Exception {
		FlatFileItemReader<Person> reader = new FlatFileItemReader<>();

		/*reader.setResource(new ClassPathResource(pathToFile));*/
		reader.setResource(new UrlResource(pathToFile));
		reader.setLineMapper(new DefaultLineMapper<Person>() {
			{

				setLineTokenizer(new DelimitedLineTokenizer() {
					{
						setNames(new String[] { "firstName", "lastName" });
					}
				});

				setFieldSetMapper(new BeanWrapperFieldSetMapper<Person>() {
					{
						setTargetType(Person.class);
					}
				});
			}
		});

		return reader;
	}

	@Bean
	public PersonItemProcessor processor() {
		return new PersonItemProcessor();
	}

	@Bean
	public JdbcBatchItemWriter<Person> writer() {
		JdbcBatchItemWriter<Person> writer = new JdbcBatchItemWriter<>();

		writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<Person>());
		writer.setSql("INSERT INTO PEOPLE (first_name, last_name) VALUES (:firstName, :lastName)");
		writer.setDataSource(dataSource);

		return writer;
	}

	@Bean
	public Job importUserJob(JobCompletionNotificationListener listener) throws Exception {
		return jobBuilderFactory.get("importUserJob")
				.incrementer(new RunIdIncrementer())
				.listener(listener)
				.flow(step1())
				.end()
				.build();
	}

	@Bean
	public Step step1() throws Exception {
		return stepBuilderFactory.get("step1")
				.<Person, Person>chunk(10)
				.reader(reader(null))
				.processor(processor())
				.writer(writer())
				.build();
	}

	@Bean
	public TaskExecutor taskExecutor() {
		return new SimpleAsyncTaskExecutor();
	}

	@Bean
	public JobLauncher jobLauncher() {
		SimpleJobLauncher jobLauncher = new SimpleJobLauncher();

		jobLauncher.setJobRepository(jobRepository);
		jobLauncher.setTaskExecutor(taskExecutor());

		return jobLauncher;
	}
}
