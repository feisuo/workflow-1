package org.neuro4j.workflow.common;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.neuro4j.workflow.loader.ClasspathWorkflowLoader;
import org.neuro4j.workflow.loader.RemoteWorkflowLoader;
import org.neuro4j.workflow.loader.WorkflowLoader;
import org.neuro4j.workflow.node.FlowParameter;

public class WorkflowLoaderMockTest {

	WorkflowConverter converter;
	
	@Mock
	URLConnection mockURLConnection;
	
	@Mock
	Workflow workflow;
	
	@Mock
	WorkflowLoader workflowLoader;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		converter = new XmlWorkflowConverter();
	}


	@Test
	public void testRemoteLoaderGetSucceeds() throws IOException, FlowExecutionException {

		String flowName = "org.neuro4j.workflow.flows.FlowForClasspathLoader-Start1";

		String remoteUrl = "http://example.com/" + flowName;

		URLStreamHandler stubHandler = new URLStreamHandler() {
			@Override
			protected URLConnection openConnection(URL u) throws IOException {
				if (!u.toString().equals(remoteUrl)) {
					fail("unexpected url encountered");
				}

				return mockURLConnection;
			}
		};

		InputStream fakeInputStream = getClass().getClassLoader()
				.getResourceAsStream(FlowParameter.parse(flowName).getFlowName().replace(".", File.separator) + WorkflowConverter.DEFAULT_EXT);

		when(mockURLConnection.getInputStream()).thenReturn(fakeInputStream);

		RemoteWorkflowLoader subject = new RemoteWorkflowLoader(converter, new ClasspathWorkflowLoader(converter),
				c -> {
					c.setRequestProperty("Accept", "text/xml");
				}, s -> s) {
			@Override
			protected URL getResource(String location) throws IOException {
				return new URL(null, location, stubHandler);
			}
		};

		Workflow actual = subject.load(remoteUrl);

		assertNotNull(actual);

		verify(mockURLConnection).getInputStream();
		verify(mockURLConnection, times(1)).setRequestProperty("Accept", "text/xml");

	}
	
	@Test
	public void testRemoteLoaderDelagatesOnFail() throws IOException, FlowExecutionException {

		String flowName = "org.neuro4j.workflow.flows.FlowForClasspathLoader-Start1";

		
		String remoteUrl = "http://mydomain.com/" + flowName;
		

		when(workflowLoader.load(remoteUrl)).thenReturn(workflow);
		
		RemoteWorkflowLoader subject = new RemoteWorkflowLoader(converter, workflowLoader,
				c -> {
					throw new RuntimeException("Connection error");
				}, s-> s) {
		};

		Workflow actual = subject.load(remoteUrl);

		assertNotNull(actual);

		verify(workflowLoader, times(1)).load(remoteUrl);
	}

}
