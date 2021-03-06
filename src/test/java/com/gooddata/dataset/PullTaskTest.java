package com.gooddata.dataset;

import org.codehaus.jackson.map.ObjectMapper;
import org.testng.annotations.Test;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PullTaskTest {

    @Test
    public void testDeserialization() throws Exception {
        final InputStream stream = getClass().getResourceAsStream("/dataset/pullTask.json");
        final PullTask task = new ObjectMapper().readValue(stream, PullTask.class);

        assertThat(task.getUri(), is("/gdc/md/PROJECT/etl/task/ID"));
    }
}
