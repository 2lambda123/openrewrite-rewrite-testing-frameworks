/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.benchmarks.java;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openrewrite.java.testing.junit5.UpdateTestAnnotation;
import org.openrewrite.java.testing.junit5.UpdateTestAnnotationOriginal;

import java.util.concurrent.TimeUnit;

@Fork(1)
@Measurement(iterations = 2)
@Warmup(iterations = 2)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.SECONDS)
@Threads(4)
public class UpdateTestAnnotationBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(UpdateTestAnnotationBenchmark.class.getSimpleName())
                .addProfiler(GCProfiler.class)
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public void updateTestAnnotation(JavaCompilationUnitState state) {
        new UpdateTestAnnotation().run(state.getSourceFiles());
    }

    @Benchmark
    public void updateTestAnnotationOriginal(JavaCompilationUnitState state) {
        new UpdateTestAnnotationOriginal().run(state.getSourceFiles());
    }

}
