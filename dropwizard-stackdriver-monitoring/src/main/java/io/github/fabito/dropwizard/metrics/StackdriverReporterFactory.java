package io.github.fabito.dropwizard.metrics;

/**
 * A base {@link ReporterFactory} for configuring metric reporters.
 * <p/>
 * Configures options common to all {@link ScheduledReporter}s.
 * <p/>
 * <b>Configuration Parameters:</b>
 * <table>
 *     <tr>
 *         <td>Name</td>
 *         <td>Default</td>
 *         <td>Description</td>
 *     </tr>
 *     <tr>
 *         <td>durationUnit</td>
 *         <td>milliseconds</td>
 *         <td>The unit to report durations as. Overrides per-metric duration units.</td>
 *     </tr>
 *     <tr>
 *         <td>rateUnit</td>
 *         <td>seconds</td>
 *         <td>The unit to report rates as. Overrides per-metric rate units.</td>
 *     </tr>
 * </table>
 */
public class StackdriverReporterFactory {
}
