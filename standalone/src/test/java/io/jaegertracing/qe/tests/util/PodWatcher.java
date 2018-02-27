package io.jaegertracing.qe.tests.util;

import static io.jaegertracing.qe.tests.ValidateTracesTest.APPLICATION_NAME;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PodWatcher implements Watcher<Pod> {
    private static final Logger logger = LoggerFactory.getLogger(PodWatcher.class.getName());
    CountDownLatch countDownLatch;

    public PodWatcher(CountDownLatch latch) {
        logger.debug("PodWatcher created");
        this.countDownLatch = latch;
    }

    @Override
    public void eventReceived(Action action, Pod pod) {
        logger.debug("Got action " + action.name() + " for pod " + pod.getMetadata().getName());  // TODO logger.debug
        List<ContainerStatus> statuses = pod.getStatus().getContainerStatuses();
        for (ContainerStatus status : statuses) {
            if (status.getName().equals(APPLICATION_NAME) && status.getState().getTerminated() != null) {
                logger.info(pod.getMetadata().getName() + " has been TERMINATED!!!!");
                countDownLatch.countDown();
            }
        }
    }

    @Override
    public void onClose(KubernetesClientException e) {
        logger.info("onClose called");
    }
}
