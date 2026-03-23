package group.phorus.exception.config

import group.phorus.metrics.commons.MetricNames
import group.phorus.metrics.commons.TagNames
import group.phorus.metrics.commons.countStatus
import group.phorus.metrics.commons.exceptionTag
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus

/**
 * Autoconfigured bean that records exception metrics using
 * [metrics-commons](https://github.com/phorus-group/metrics-commons).
 *
 * Only active when:
 * - A `io.micrometer.core.instrument.MeterRegistry` bean exists (e.g. via Spring Boot Actuator)
 * - The property `group.phorus.exception.metrics.enabled` is `true` (default)
 *
 * Produces a counter named [MetricNames.HTTP_SERVER_EXCEPTIONS] with tags:
 * - [TagNames.TYPE]: the simple class name of the exception (e.g. `BadRequest`, `NotFound`), extracted via [exceptionTag]
 * - [TagNames.STATUS_CODE]: the HTTP status code (e.g. `400`, `404`)
 * - [TagNames.STATUS_FAMILY]: the status family (e.g. `4xx`, `5xx`)
 *
 * Consuming projects can disable metrics by setting:
 * ```yaml
 * group:
 *   phorus:
 *     exception:
 *       metrics:
 *         enabled: false
 * ```
 */
@AutoConfiguration
@ConditionalOnBean(MeterRegistry::class)
@ConditionalOnProperty(
    prefix = "group.phorus.exception.metrics",
    name = ["enabled"],
    matchIfMissing = true,
)
class MetricsRecorder(
    private val meterRegistry: MeterRegistry,
) {

    /**
     * Records an exception occurrence as a counter metric.
     *
     * Uses [exceptionTag] to safely extract the exception's simple class name (null-safe, returns "None" if null),
     * and [countStatus] to automatically add both [TagNames.STATUS_FAMILY] (e.g. "4xx") and [TagNames.STATUS_CODE] (e.g. "404") tags.
     *
     * @param exception the caught exception (null-safe).
     * @param httpStatus the HTTP status returned to the client.
     */
    fun record(exception: Throwable?, httpStatus: HttpStatus) {
        meterRegistry.countStatus(
            name = MetricNames.HTTP_SERVER_EXCEPTIONS,
            statusCode = httpStatus.value(),
            TagNames.TYPE to exceptionTag(exception),
        )
    }
}
