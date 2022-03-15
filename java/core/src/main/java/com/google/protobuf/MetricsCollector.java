package com.google.protobuf;

import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MetricsCollector {
  private static final String INPUT_FEATURE_SUFFIX = "-featureLatencyNs";
  private static final String OUTPUT_FEATURE_SUFFIX = "-outputLatencyNs";

  private static final Logger logger = Logger.getLogger(MetricsCollector.class.getName());

  public final MetricRegistry _registry = new MetricRegistry();
  private static MetricsCollector METRICS_COLLECTOR_INSTANCE;

  private MetricsCollector() {
    try {
      File temporaryWorkingDirectory = Files.createTempDirectory("gRPCSerDeSer_latency_").toFile();
      logger.info("Start to write gRPCSerDeSer latency report to: " + temporaryWorkingDirectory.getAbsolutePath());
      CsvReporter csvReporter = CsvReporter.forRegistry(_registry)
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.NANOSECONDS)
          .build(temporaryWorkingDirectory);
      csvReporter.start(1, TimeUnit.MINUTES);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to create temp directory for tf inference latency report");
    }
  }

  public static MetricsCollector getMetricsCollectorInstance() {
    if(METRICS_COLLECTOR_INSTANCE == null) {
      METRICS_COLLECTOR_INSTANCE = new MetricsCollector();
    }

    return METRICS_COLLECTOR_INSTANCE;
  }

  void reportLatency(String key, long latency) {
    _registry.histogram(key).update(latency);
  }

  public void createNewHistogramIfNotPresent(String metricName, boolean isInput) {
    String fullMetricName = metricName + (isInput ? INPUT_FEATURE_SUFFIX : OUTPUT_FEATURE_SUFFIX);
    if(!_registry.getMetrics().containsKey(fullMetricName)) {
      _registry.register(fullMetricName, new Histogram(new ExponentiallyDecayingReservoir(1028, 0.075)));
    }
  }
}
