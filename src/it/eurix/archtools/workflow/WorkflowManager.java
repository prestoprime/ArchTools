/**
 * WorkflowManager.java
 * Author: Francesco Rosso (rosso@eurix.it)
 * 
 * This file is part of PrestoPRIME Preservation Platform (P4).
 * 
 * Copyright (C) 2009-2012 EURIX Srl, Torino, Italy
 *  
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.eurix.archtools.workflow;

import it.eurix.archtools.workflow.exceptions.TaskExecutionFailedException;
import it.eurix.archtools.workflow.exceptions.UndefinedServiceException;
import it.eurix.archtools.workflow.exceptions.UndefinedWorkflowException;
import it.eurix.archtools.workflow.exceptions.WorkflowExecutionFailedException;
import it.eurix.archtools.workflow.jaxb.StatusType;
import it.eurix.archtools.workflow.jaxb.WfDescriptor;
import it.eurix.archtools.workflow.jaxb.WfStatus;
import it.eurix.archtools.workflow.jaxb.WfDescriptor.Services.Service;
import it.eurix.archtools.workflow.jaxb.WfDescriptor.Workflows.Workflow;
import it.eurix.archtools.workflow.jaxb.WfDescriptor.Workflows.Workflow.SParam;
import it.eurix.archtools.workflow.jaxb.WfDescriptor.Workflows.Workflow.Task;
import it.eurix.archtools.workflow.jaxb.WfStatus.Params;
import it.eurix.archtools.workflow.jaxb.WfStatus.Result;
import it.eurix.archtools.workflow.jaxb.WfStatus.TimeTable;
import it.eurix.archtools.workflow.jaxb.WfStatus.TimeTable.TaskReport;
import it.eurix.archtools.workflow.plugin.WfServiceInterface;
import it.eurix.archtools.workflow.plugin.WfServiceScanner;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WorkflowManager {

	protected static final Logger logger = LoggerFactory.getLogger(WorkflowManager.class);

	private WorkflowPersistenceManager wfPersistence;

	protected WorkflowManager(WorkflowPersistenceManager wfPersistence) {
		this.wfPersistence = wfPersistence;
		this.wfPersistence.getWfDescriptor();
	}

	@Deprecated
	public WfDescriptor getWfDescriptor() {
		return wfPersistence.getWfDescriptor();
	}
	
	@Deprecated
	public void setWfDescriptor(File descriptor) {
		wfPersistence.setWfDescriptor(descriptor);
	}
	
	@Deprecated
	public WfStatus getWfStatus(String jobID) {
		return wfPersistence.getWfStatus(jobID);
	}
	
	@Deprecated
	public void setWfStatus(WfStatus wfStatus) {
		wfPersistence.setWfStatus(wfStatus);
	}
	
	@Deprecated
	public void deleteWfStatus(String jobID) {
		wfPersistence.deleteWfStatus(jobID);
	}

	public List<String> getWorkflows() {
		WfDescriptor wfDescriptor = wfPersistence.getWfDescriptor();

		List<String> workflows = new ArrayList<>();
		for (Workflow workflow : wfDescriptor.getWorkflows().getWorkflow())
			workflows.add(workflow.getId());
		return workflows;
	}

	public String executeWorkflow(String wfID, Map<String, String> dParamsString, Map<String, File> dParamsFile) throws UndefinedWorkflowException {
		logger.debug("Searching workflow " + wfID);
		
		Workflow workflow = this.getWorkflow(wfID);

		logger.debug("Found workflow " + wfID);
		logger.debug("Loading static parameters for workflow " + wfID);

		Map<String, String> sParams = new HashMap<>();
		for (SParam param : workflow.getSParam())
			sParams.put(param.getKey(), param.getValue());

		logger.debug("Loaded static parameters for workflow " + wfID);
		logger.debug("Loading tasks for workflow " + wfID);

		List<List<Task>> tasks = this.getAllTasks(workflow);

		logger.debug("Loaded tasks for workflow " + wfID);

		String jobID = "job-" + UUID.randomUUID();

		Params params = new Params();
		for (Entry<String, String> entry : sParams.entrySet()) {
			WfStatus.Params.SParam sParam = new WfStatus.Params.SParam();
			sParam.setKey(entry.getKey());
			sParam.setValue(entry.getValue());
			params.getSParam().add(sParam);
		}
		for (Entry<String, String> entry : dParamsString.entrySet()) {
			WfStatus.Params.DParamString dParamString = new WfStatus.Params.DParamString();
			dParamString.setKey(entry.getKey());
			dParamString.setValue(entry.getValue());
			params.getDParamString().add(dParamString);
		}
		for (Entry<String, File> entry : dParamsFile.entrySet()) {
			WfStatus.Params.DParamFile dParamFile = new WfStatus.Params.DParamFile();
			dParamFile.setKey(entry.getKey());
			dParamFile.setValue(entry.getValue().getAbsolutePath());
			params.getDParamFile().add(dParamFile);
		}

		WfStatus wfStatus = new WfStatus();
		wfStatus.setId(jobID);
		wfStatus.setWorkflow(wfID);
		wfStatus.setTotalSteps(tasks.size());
		wfStatus.setLastCompletedStep(0);
		wfStatus.setStatus(StatusType.WAITING);
		wfStatus.setParams(params);
		wfStatus.setTimeTable(new TimeTable());

		wfPersistence.setWfStatus(wfStatus);

		logger.debug("Starting new workflow executor");

		WorkflowExecutor wfExecutor = new WorkflowExecutor(tasks, wfStatus, sParams, dParamsString, dParamsFile);
		wfExecutor.setName(jobID);
		wfExecutor.start();

		logger.debug("Started workflow executor " + wfID);

		return jobID;
	}

	private Workflow getWorkflow(String id) throws UndefinedWorkflowException {
		for (Workflow workflow : wfPersistence.getWfDescriptor().getWorkflows().getWorkflow())
			if (workflow.getId().equals(id))
				return workflow;
		throw new UndefinedWorkflowException("Unable to find a workflow with id " + id);
	}

	private List<List<Task>> getAllTasks(Workflow workflow) {
		List<List<Task>> tasks = new ArrayList<List<Task>>();
		List<Task> tmpTasks;
		int order = 1;
		while (!(tmpTasks = this.getTasksByOrder(workflow, order++)).isEmpty()) {
			tasks.add(tmpTasks);
		}
		return tasks;
	}

	private List<Task> getTasksByOrder(Workflow workflow, int order) {
		List<Task> tasks = new ArrayList<Task>();
		for (Task task : workflow.getTask())
			if (task.getStep() == order)
				tasks.add(task);
		return tasks;
	}

	/**
	 * @param The
	 *            service ID to scan for
	 * @return The service object, representing the service class (ant method) to be executed
	 * @throws UndefinedServiceException
	 *             Thrown if it's not possible to find a service that binds the request parameters
	 */
	@Deprecated
	private Service getService(String id) throws UndefinedServiceException {
		// search in wfDescriptor <services> *legacy* section
		for (Service service : wfPersistence.getWfDescriptor().getServices().getService())
			if (service.getId().equals(id))
				return service;
		throw new UndefinedServiceException("Unable to find a service with id " + id);
	}

	class WorkflowExecutor extends Thread {

		private List<List<Task>> tasks;
		private WfStatus wfStatus;
		private Map<String, String> sParams;
		private Map<String, String> dParamString;
		private Map<String, File> dParamFile;

		public WorkflowExecutor(List<List<Task>> tasks, WfStatus wfStatus, Map<String, String> sParams, Map<String, String> dParamString, Map<String, File> dParamFile) {
			this.tasks = tasks;
			this.wfStatus = wfStatus;
			this.sParams = sParams;
			this.dParamString = dParamString;
			this.dParamFile = dParamFile;

			WorkflowManager.this.wfPersistence.setWfStatus(wfStatus);
		}

		@Override
		public void run() {
			logger.debug("Executing tasks for workflow " + this.getName());

			int step = 0;

			wfStatus.setStatus(StatusType.RUNNING);
			wfStatus.setDuration(0L);
			try {
				wfStatus.setStartup(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
			} catch (DatatypeConfigurationException e) {
				wfStatus.setStatus(StatusType.FAILED);
				Result result = new Result();
				result.setValue(e.getMessage());
				wfStatus.setResult(result);
				return;
			} finally {
				WorkflowManager.this.wfPersistence.setWfStatus(wfStatus);
			}

			while (!tasks.isEmpty()) {
				step++;

				List<Task> tmpTasks = tasks.remove(0);
				try {
					this.executeTasks(tmpTasks);

					wfStatus.setLastCompletedStep(step);
				} catch (WorkflowExecutionFailedException e) {
					e.printStackTrace();

					wfStatus.setStatus(StatusType.FAILED);
					wfStatus.setDuration(System.currentTimeMillis() - wfStatus.getStartup().toGregorianCalendar().getTimeInMillis());

					Result result = new Result();
					result.setValue(e.getMessage());
					wfStatus.setResult(result);

					logger.debug("Executed job " + this.getName() + " - FAILED");

					return;
				} finally {
					WorkflowManager.this.wfPersistence.setWfStatus(wfStatus);
				}
			}

			wfStatus.setStatus(StatusType.COMPLETED);
			wfStatus.setDuration(System.currentTimeMillis() - wfStatus.getStartup().toGregorianCalendar().getTimeInMillis());

			String resultValue = dParamString.get("result");
			if (resultValue != null) {
				Result result = new Result();
				result.setValue(resultValue);
				wfStatus.setResult(result);
			}
			WorkflowManager.this.wfPersistence.setWfStatus(wfStatus);

			logger.debug("Executed job " + this.getName() + " - SUCCESSFUL");
		}

		private void executeTasks(List<Task> tasks) throws WorkflowExecutionFailedException {
			List<TaskExecutor> executors = new ArrayList<>();
			for (Task task : tasks) {
				TaskExecutor executor = new TaskExecutor(task);
				executor.start();
				executors.add(executor);
			}
			for (TaskExecutor executor : executors) {
				try {
					executor.join();

					if (executor.isFailed() && executor.getTask().isCritical()) {
						throw new WorkflowExecutionFailedException("Critical task failed\n" + executor.getMessage());
					} else {
						TaskReport taskReport = new TaskReport();
						taskReport.setService(executor.task.getService());
						taskReport.setStep(executor.task.getStep());
						try {
							taskReport.setStartup(DatatypeFactory.newInstance().newXMLGregorianCalendar(executor.startup));
						} catch (DatatypeConfigurationException e) {
							throw new WorkflowExecutionFailedException("Task commit failed\n" + executor.getMessage());
						}
						taskReport.setDuration(executor.duration);

						wfStatus.getTimeTable().getTaskReport().add(taskReport);
						wfStatus.setDuration(System.currentTimeMillis() - wfStatus.getStartup().toGregorianCalendar().getTimeInMillis());
						WorkflowManager.this.wfPersistence.setWfStatus(wfStatus);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		class TaskExecutor extends Thread {

			private Task task;
			private GregorianCalendar startup;
			private long duration;
			private int attempts;
			private WfServiceInterface service;
			private boolean failed;
			private String message;

			public TaskExecutor(Task task) {
				this.task = task;
				this.startup = new GregorianCalendar();
				this.duration = 0;
				this.attempts = task.getAttempts();
				try {
					// new annotated services loader
					final Method method = WfServiceScanner.getInstance().getService(task.getService());
					final Object pluginClass = method.getDeclaringClass().newInstance();
					this.service = new WfServiceInterface() {

						@Override
						public void execute(Map<String, String> sParams, Map<String, String> dParamsString, Map<String, File> dParamsFile) throws TaskExecutionFailedException {
							try {
								method.invoke(pluginClass, sParams, dParamsString, dParamsFile);
							} catch (InvocationTargetException e) {
								if (e.getTargetException() instanceof TaskExecutionFailedException) {
									logger.error("TaskExecutionFailedException thrown by method on class loaded with the new annotated services loader...");
									throw (TaskExecutionFailedException) e.getTargetException();
								} else {
									logger.error("RuntimeException thrown by method on class loaded with the new annotated services loader...");
									throw new RuntimeException(e.getTargetException());
								}
							} catch (IllegalAccessException | IllegalArgumentException e) {
								throw new TaskExecutionFailedException("Unable to invoke method on class loaded with the new annotated services loader. Returned with message: " + e.getMessage() + "...");
							}
						}
					};
					this.failed = false;
				} catch (UndefinedServiceException | IllegalAccessException | InstantiationException e1) {
					logger.warn(e1.getMessage());

					// not found/not instantiable with annotated services loader
					// try with legacy wfDescriptor services loader
					try {
						Class<?> taskClass = Class.forName(WorkflowManager.this.getService(task.getService()).getClazz());
						this.service = (WfServiceInterface) taskClass.newInstance();
						this.failed = false;
					} catch (UndefinedServiceException | ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException e2) {
						e2.printStackTrace();
						this.failed = true;
						this.message = "(Constructor)" + e2.getMessage() + "\n";
					}
				}
			}

			public Task getTask() {
				return task;
			}

			public boolean isFailed() {
				return failed;
			}

			public String getMessage() {
				return message;
			}

			@Override
			public void run() {
				if (!failed) {
					if (attempts-- != 0) {
						try {
							service.execute(WorkflowExecutor.this.sParams, WorkflowExecutor.this.dParamString, WorkflowExecutor.this.dParamFile);
						} catch (TaskExecutionFailedException | RuntimeException e) {
							message += "(" + attempts + ") " + e.getMessage() + "\n";
							e.printStackTrace();
							this.run();
						}
					} else {
						failed = true;
					}
				}
				this.duration = (int) (System.currentTimeMillis() - startup.getTimeInMillis());
			}
		}
	}
}
