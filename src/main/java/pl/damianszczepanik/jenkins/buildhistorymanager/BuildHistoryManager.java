package pl.damianszczepanik.jenkins.buildhistorymanager;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import hudson.Util;
import hudson.model.Job;
import hudson.model.Run;
import jenkins.model.BuildDiscarder;
import org.kohsuke.stapler.DataBoundConstructor;
import pl.damianszczepanik.jenkins.buildhistorymanager.model.Rule;

import static java.lang.String.format;


/**
 * Custom implementation of {@link BuildDiscarder}
 *
 * @author Damian Szczepanik (damianszczepanik@github)
 * @see hudson.tasks.LogRotator
 */
public class BuildHistoryManager extends BuildDiscarder
{
	private static final Logger LOG = Logger.getLogger(BuildHistoryManager.class.getName());

	private final List<Rule> rules;

	@DataBoundConstructor
	public BuildHistoryManager(List<Rule> rules)
	{
		this.rules = Util.fixNull(rules);
	}

	public List<Rule> getRules()
	{
		return rules;
	}

	/**
	 * Entry point for the discarding process. Iterates over the completed builds and rules.
	 *
	 * @see BuildDiscarder#perform(Job)
	 * @see Job#logRotate()
	 */
	@Override
	public void perform(Job<?, ?> job) throws IOException, InterruptedException
	{
		String a = Arrays.stream(Thread.currentThread().getStackTrace())
			.map(StackTraceElement::toString)
			.map("\n"::concat)
			.reduce("", String::concat);
		LOG.info("Execution stack trace for thread [" + Thread.currentThread().getId() + "]" + a);

		// reset counters of matched builds
		for (Rule rule : rules)
		{
			rule.initialize();
		}

		Run<?, ?> run = job.getLastCompletedBuild();

		final Run<?, ?> triggeringBuild = job.getLastCompletedBuild();
		LOG.info(format("Triggering build [%s]", triggeringBuild.getFullDisplayName()));

		int buildCount = 1;

		// for each completed build...
		do
		{
			LOG.info(format("Processing rules for [%s] (count %s) from [%s]", run.getFullDisplayName(), buildCount++,
			                triggeringBuild.getFullDisplayName()));

			for (int i = 0; i < rules.size(); i++)
			{
				LOG.info(format("Evaluating rule [%s] of [%s] from [%s]", (i + 1), run.getFullDisplayName(),
				                triggeringBuild.getFullDisplayName()));

				Rule rule = rules.get(i);
				if (rule.validateConditions(run))
				{
					LOG.info(format("Processing actions for rule [%s] of [%s] from [%s]", (i + 1), run.getFullDisplayName(),
					                triggeringBuild.getFullDisplayName()));

					rule.performActions(run);

					// if other rules should not be proceed, shift to next build
					if (!rule.getContinueAfterMatch())
					{
						LOG.info(format("Will NOT continue processing rules for [%s] from [%s]", run.getFullDisplayName(),
						                triggeringBuild.getFullDisplayName()));
						break;
					}
					else
					{
						LOG.info(format("Will continue processing rules for [%s] from [%s]", run.getFullDisplayName(),
						                triggeringBuild.getFullDisplayName()));
					}
				}
			}

			// validateConditions rules for previous build - completed in case some previous are still building
			run = run.getPreviousCompletedBuild();
			// stop when the iteration reach the oldest build
		}
		while (run != null);
	}
}
