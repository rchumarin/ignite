/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.examples.ml.regression.linear;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.ml.dataset.feature.extractor.impl.FeatureLabelExtractorWrapper;
import org.apache.ignite.ml.math.functions.IgniteBiFunction;
import org.apache.ignite.ml.math.primitives.vector.Vector;
import org.apache.ignite.ml.preprocessing.minmaxscaling.MinMaxScalerPreprocessor;
import org.apache.ignite.ml.preprocessing.minmaxscaling.MinMaxScalerTrainer;
import org.apache.ignite.ml.regressions.linear.LinearRegressionLSQRTrainer;
import org.apache.ignite.ml.regressions.linear.LinearRegressionModel;
import org.apache.ignite.ml.selection.scoring.evaluator.Evaluator;
import org.apache.ignite.ml.selection.scoring.metric.regression.RegressionMetrics;
import org.apache.ignite.ml.util.MLSandboxDatasets;
import org.apache.ignite.ml.util.SandboxMLCache;

import java.io.FileNotFoundException;

/**
 * Run linear regression model based on <a href="http://web.stanford.edu/group/SOL/software/lsqr/">LSQR algorithm</a>
 * ({@link LinearRegressionLSQRTrainer}) over cached dataset that was created using
 * a minmaxscaling preprocessor ({@link MinMaxScalerTrainer}, {@link MinMaxScalerPreprocessor}).
 * <p>
 * Code in this example launches Ignite grid, fills the cache with simple test data, and defines minmaxscaling
 * trainer and preprocessor.</p>
 * <p>
 * After that it trains the linear regression model based on the specified data that has been processed
 * using minmaxscaling.</p>
 * <p>
 * Finally, this example loops over the test set of data points, applies the trained model to predict predict the target
 * value and compares prediction to expected outcome (ground truth).</p>
 * <p>
 * You can change the test data used in this example and re-run it to explore this algorithm further.</p>
 */
public class LinearRegressionLSQRTrainerWithMinMaxScalerExample {
    /** Run example. */
    public static void main(String[] args) throws FileNotFoundException {
        System.out.println();
        System.out.println(">>> Linear regression model with Min Max Scaling preprocessor over cached dataset usage example started.");
        // Start ignite grid.
        try (Ignite ignite = Ignition.start("examples/config/example-ignite.xml")) {
            System.out.println(">>> Ignite grid started.");

            IgniteCache<Integer, Vector> dataCache = null;
            try {
                dataCache = new SandboxMLCache(ignite).fillCacheWith(MLSandboxDatasets.MORTALITY_DATA);

                System.out.println(">>> Create new MinMaxScaler trainer object.");
                MinMaxScalerTrainer<Integer, Vector> minMaxScalerTrainer = new MinMaxScalerTrainer<>();

                System.out.println(">>> Perform the training to get the MinMaxScaler preprocessor.");
                IgniteBiFunction<Integer, Vector, Vector> preprocessor = minMaxScalerTrainer.fit(
                    ignite,
                    dataCache,
                    (k, v) -> v.copyOfRange(1, v.size())
                );

                System.out.println(">>> Create new linear regression trainer object.");
                LinearRegressionLSQRTrainer trainer = new LinearRegressionLSQRTrainer();

                System.out.println(">>> Perform the training to get the model.");

                final IgniteBiFunction<Integer, Vector, Double> lbExtractor = (k, v) -> v.get(0);

                LinearRegressionModel mdl = trainer.fit(ignite, dataCache, FeatureLabelExtractorWrapper.wrap(preprocessor, lbExtractor)); //TODO: IGNITE-11581

                System.out.println(">>> Linear regression model: " + mdl);

                double rmse = Evaluator.evaluate(
                    dataCache,
                    mdl,
                    preprocessor,
                    lbExtractor,
                    new RegressionMetrics()
                );

                System.out.println("\n>>> Rmse = " + rmse);

                System.out.println(">>> ---------------------------------");
                System.out.println(">>> Linear regression model with MinMaxScaler preprocessor over cache based dataset usage example completed.");
            } finally {
                dataCache.destroy();
            }
        }
    }
}
