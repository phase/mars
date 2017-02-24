package xyz.jadonfowler.compiler

import org.junit.runners.Suite
import org.junit.runners.model.RunnerBuilder
import org.junit.runners.model.RunnerScheduler
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ParallelSuite(klass: Class<*>, builder: RunnerBuilder) : Suite(klass, builder) {

    init {
        setScheduler(object : RunnerScheduler {
            val service = Executors.newFixedThreadPool(16)

            override fun schedule(childStatement: Runnable) {
                service.submit(childStatement)
            }

            override fun finished() {
                try {
                    service.shutdown()
                    service.awaitTermination(java.lang.Long.MAX_VALUE, TimeUnit.NANOSECONDS)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
        })
    }

}
