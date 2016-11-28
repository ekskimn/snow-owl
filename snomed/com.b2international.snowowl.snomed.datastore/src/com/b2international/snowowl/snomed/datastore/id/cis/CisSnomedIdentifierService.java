/*
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.b2international.snowowl.snomed.datastore.id.cis;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.partition;
import static java.util.Collections.singleton;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.commons.time.TimeUtil;
import com.b2international.snowowl.core.IDisposableService;
import com.b2international.snowowl.core.api.SnowowlRuntimeException;
import com.b2international.snowowl.core.date.DateFormats;
import com.b2international.snowowl.core.date.Dates;
import com.b2international.snowowl.core.exceptions.BadRequestException;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.snomed.datastore.config.SnomedIdentifierConfiguration;
import com.b2international.snowowl.snomed.datastore.id.AbstractSnomedIdentifierService;
import com.b2international.snowowl.snomed.datastore.id.SnomedIdentifiers;
import com.b2international.snowowl.snomed.datastore.id.cis.request.BulkDeprecationData;
import com.b2international.snowowl.snomed.datastore.id.cis.request.BulkGenerationData;
import com.b2international.snowowl.snomed.datastore.id.cis.request.BulkPublicationData;
import com.b2international.snowowl.snomed.datastore.id.cis.request.BulkRegistrationData;
import com.b2international.snowowl.snomed.datastore.id.cis.request.BulkReleaseData;
import com.b2international.snowowl.snomed.datastore.id.cis.request.BulkReservationData;
import com.b2international.snowowl.snomed.datastore.id.cis.request.DeprecationData;
import com.b2international.snowowl.snomed.datastore.id.cis.request.GenerationData;
import com.b2international.snowowl.snomed.datastore.id.cis.request.PublicationData;
import com.b2international.snowowl.snomed.datastore.id.cis.request.Record;
import com.b2international.snowowl.snomed.datastore.id.cis.request.RegistrationData;
import com.b2international.snowowl.snomed.datastore.id.cis.request.ReleaseData;
import com.b2international.snowowl.snomed.datastore.id.cis.request.RequestData;
import com.b2international.snowowl.snomed.datastore.id.cis.request.ReservationData;
import com.b2international.snowowl.snomed.datastore.id.reservations.ISnomedIdentiferReservationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

/**
 * CIS (IHTSDO) based implementation of the identifier service.
 * 
 * @since 4.5
 */
public class CisSnomedIdentifierService extends AbstractSnomedIdentifierService implements IDisposableService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CisSnomedIdentifierService.class);
	
	private static final String INT_NAMESPACE = "INT";
	private static final int BULK_LIMIT = 1000;
	private static final int BULK_GET_LIMIT = 3000;

	private final long numberOfPollTries;
	private final long numberOfReauthTries;
	private final long timeBetweenPollTries;

	private final String clientKey;
	private final ObjectMapper mapper;

	private CisClient client;
	private boolean disposed = false;

	public CisSnomedIdentifierService(final SnomedIdentifierConfiguration conf, final ISnomedIdentiferReservationService reservationService,
			final ObjectMapper mapper) {
		super(reservationService, conf);
		this.clientKey = conf.getCisClientSoftwareKey();
		this.numberOfPollTries = conf.getCisNumberOfPollTries();
		this.timeBetweenPollTries = conf.getCisTimeBetweenPollTries();
		this.numberOfReauthTries = conf.getCisNumberOfReauthTries();
		this.mapper = mapper;
		this.client = new CisClient(conf, mapper);

		// Log in at startup, and keep the token as long as possible
		login();
	}

	// GENERATE
	
	@Override
	public String generate(final String namespace, final ComponentCategory category) {

		HttpPost request = null;

		try {
			LOGGER.debug("Sending single {} ID generation request for namespace {}", category.getDisplayName(),
					getSafeNamespaceDisplayName(namespace));
			request = httpPost("sct/generate", createGenerationData(namespace, category));
			return extractSctId(execute(request)).getSctid();
		} catch (IOException e) {
			throw new SnowowlRuntimeException(String.format("Unable to generate single %s ID for namespace %s", category.getDisplayName(),
					getSafeNamespaceDisplayName(namespace)), e);
		} finally {
			release(request);
		}

	}

	@Override
	public Collection<String> generate(final String namespace, final ComponentCategory category, final int quantity) {

		HttpPost bulkRequest = null;

		try {

			LOGGER.debug("Sending bulk {} ID generation request for namespace {} with size {}", category.getDisplayName(),
					getSafeNamespaceDisplayName(namespace), quantity);

			bulkRequest = httpPost("sct/bulk/generate", createBulkGenerationData(namespace, category, quantity));
			final String jobId = extractJobId(execute(bulkRequest));

			waitForJob(jobId);

			return getComponentIdsFromBulkJob(jobId);

		} catch (IOException e) {
			throw new SnowowlRuntimeException(String.format("Unable to bulk generate %s ID for namespace %s with size %s", category.getDisplayName(),
					getSafeNamespaceDisplayName(namespace), quantity), e);
		} finally {
			release(bulkRequest);
		}
	}

	// REGISTER
	
	@Override
	public void register(final String componentId) {
		
		Collection<String> idsToRegister = validateForRegistration(Collections.singleton(getSctId(componentId)));
		
		if (idsToRegister.isEmpty()) {
			return;
		}
		
		String idToRegister = Iterables.getOnlyElement(idsToRegister);
		
		if (!idToRegister.equals(componentId)) {
			throw new SnowowlRuntimeException(String.format("ID (%s) and SctId (%s) does not match for registration", componentId, idToRegister));
		}

		HttpPost request = null;
		
		try {
			
			LOGGER.debug("Sending single registration request for ID {}", idToRegister);
			request = httpPost("sct/register", createRegistrationData(idToRegister));
			
			execute(request);
			
		} catch (IOException e) {
			throw new SnowowlRuntimeException(String.format("Unable to register ID %s", idToRegister), e);
		} finally {
			release(request);
		}
	}
	
	@Override
	public void register(final Collection<String> componentIds) {

		Collection<String> idsToRegister = validateForRegistration(getSctIds(componentIds));

		if (idsToRegister.isEmpty()) {
			return;
		}

		final Multimap<String, String> namespaceToIdsMap = toNamespaceMap(idsToRegister);

		for (Entry<String, Collection<String>> entry : namespaceToIdsMap.asMap().entrySet()) {

			final String namespace = entry.getKey();

			for (List<String> ids : partition(newArrayList(entry.getValue()), BULK_LIMIT)) {

				HttpPost bulkRequest = null;
				
				try {

					LOGGER.debug("Sending bulk registration request for namespace {} with size {}", namespace, ids.size());

					bulkRequest = httpPost("sct/bulk/register", createBulkRegistrationData(namespace, ids));

					waitForJob(extractJobId(execute(bulkRequest)));

				} catch (IOException e) {
					throw new SnowowlRuntimeException(
							String.format("Unable to bulk register IDs for namespace %s with size %s", namespace, ids.size()), e);
				} finally {
					release(bulkRequest);
				}
			}
		}
	}

	// RESERVE
	
	@Override
	public String reserve(final String namespace, final ComponentCategory category) {

		HttpPost request = null;

		try {

			LOGGER.debug("Sending single {} ID reservation request for namespace {}", category.getDisplayName(),
					getSafeNamespaceDisplayName(namespace));

			request = httpPost("sct/reserve", createReservationData(namespace, category));

			return extractSctId(execute(request)).getSctid();

		} catch (IOException e) {
			throw new SnowowlRuntimeException(
					String.format("Unable to reserve %s ID for namespace %s", category.getDisplayName(), getSafeNamespaceDisplayName(namespace)), e);
		} finally {
			release(request);
		}
	}

	@Override
	public Collection<String> reserve(String namespace, ComponentCategory category, int quantity) {

		HttpPost bulkRequest = null;

		try {

			LOGGER.debug("Sending bulk {} ID reservation request for namespace {} with size {}", category.getDisplayName(),
					getSafeNamespaceDisplayName(namespace), quantity);

			bulkRequest = httpPost("sct/bulk/reserve", createBulkReservationData(namespace, category, quantity));

			String jobId = extractJobId(execute(bulkRequest));

			waitForJob(jobId);

			return getComponentIdsFromBulkJob(jobId);

		} catch (IOException e) {
			throw new SnowowlRuntimeException(String.format("Unable to bulk reserve %s IDs for namespace %s with size %s", category.getDisplayName(),
					getSafeNamespaceDisplayName(namespace), quantity), e);
		} finally {
			release(bulkRequest);
		}
	}

	// DEPRECATE
	
	@Override
	public void deprecate(final String componentId) {
		
		Collection<String> idsToDeprecate = validateForDeprecation(singleton(getSctId(componentId)));
		
		if (idsToDeprecate.isEmpty()) {
			return;
		}
		
		String idToDeprecate = Iterables.getOnlyElement(idsToDeprecate);
		
		if (!idToDeprecate.equals(componentId)) {
			throw new SnowowlRuntimeException(String.format("ID (%s) and SctId (%s) does not match for deprecation", componentId, idToDeprecate));
		}
		
		HttpPut request = null;
		
		try {
			
			LOGGER.debug("Sending single deprecation request for ID {}", idToDeprecate);

			request = httpPut("sct/deprecate", createDeprecationData(idToDeprecate));
			
			execute(request);
			
		} catch (IOException e) {
			throw new SnowowlRuntimeException(String.format("Unable to deprecate ID %s", idToDeprecate), e);
		} finally {
			release(request);
		}
	}

	@Override
	public void deprecate(final Collection<String> componentIds) {

		Collection<String> idsToDeprecate = validateForDeprecation(getSctIds(componentIds));

		if (idsToDeprecate.isEmpty()) {
			return;
		}

		final Multimap<String, String> namespaceToIdsMap = toNamespaceMap(idsToDeprecate);

		for (Entry<String, Collection<String>> entry : namespaceToIdsMap.asMap().entrySet()) {

			String namespace = entry.getKey();

			for (List<String> ids : partition(newArrayList(entry.getValue()), BULK_LIMIT)) {

				HttpPut bulkRequest = null;
				
				try {

					LOGGER.debug("Sending bulk deprecation request for namespace {} with size {}", namespace, ids.size());

					bulkRequest = httpPut("sct/bulk/deprecate", createBulkDeprecationData(namespace, ids));

					waitForJob(extractJobId(execute(bulkRequest)));

				} catch (IOException e) {
					throw new SnowowlRuntimeException(
							String.format("Unable to bulk deprecate IDs for namespace %s with size %s", namespace, ids.size()), e);
				} finally {
					release(bulkRequest);
				}
			}
		}
	}

	// RELEASE
	
	@Override
	public void release(final String componentId) {
		
		Collection<String> idsToRelease = validateForReleasing(singleton(getSctId(componentId)));
		
		if (idsToRelease.isEmpty()) {
			return;
		}
		
		String idToRelease = Iterables.getOnlyElement(idsToRelease);
		
		if (!idToRelease.equals(componentId)) {
			throw new SnowowlRuntimeException(String.format("ID (%s) and SctId (%s) does not match for releasing", componentId, idToRelease));
		}
		
		HttpPut request = null;
		
		try {
			
			LOGGER.debug("Sending single release request for component ID {}", idToRelease);

			request = httpPut("sct/release", createReleaseData(idToRelease));
			
			execute(request);
			
		} catch (IOException e) {
			throw new SnowowlRuntimeException(String.format("Unable to release ID %s", idToRelease), e);
		} finally {
			release(request);
		}
	}

	@Override
	public void release(final Collection<String> componentIds) {

		Collection<String> idsToRelease = validateForReleasing(getSctIds(componentIds));

		if (idsToRelease.isEmpty()) {
			return;
		}

		final Multimap<String, String> namespaceToIdsMap = toNamespaceMap(idsToRelease);

		for (Entry<String, Collection<String>> entry : namespaceToIdsMap.asMap().entrySet()) {

			String namespace = entry.getKey();

			for (List<String> ids : partition(newArrayList(entry.getValue()), BULK_LIMIT)) {

				HttpPut bulkRequest = null;
				
				try {

					LOGGER.debug("Sending bulk release request for namespace {} with size {}", namespace, ids.size());

					bulkRequest = httpPut("sct/bulk/release", createBulkReleaseData(namespace, ids));

					waitForJob(extractJobId(execute(bulkRequest)));

				} catch (IOException e) {
					throw new SnowowlRuntimeException(
							String.format("Unable to bulk release IDs for namespace %s with size %s", namespace, ids.size()), e);
				} finally {
					release(bulkRequest);
				}
			}
		}
	}

	@Override
	public void publish(final String componentId) {
		
		Collection<String> idsToPublish = validateForPublishing(singleton(getSctId(componentId)));
		
		if (idsToPublish.isEmpty()) {
			return;
		}
		
		String idToPublish = Iterables.getOnlyElement(idsToPublish);
		
		if (!idToPublish.equals(componentId)) {
			throw new SnowowlRuntimeException(String.format("ID (%s) and SctId (%s) does not match for publishing", componentId, idToPublish));
		}
		
		HttpPut request = null;
		
		try {
			
			LOGGER.debug("Sending single publish request for component ID {}", idToPublish);

			request = httpPut("sct/publish", createPublishData(idToPublish));
			
			execute(request);
			
		} catch (IOException e) {
			throw new SnowowlRuntimeException(String.format("Unable to publish ID %s", idToPublish), e);
		} finally {
			release(request);
		}
	}

	@Override
	public void publish(final Collection<String> componentIds) {

		Collection<String> idsToPublish = validateForPublishing(getSctIds(componentIds));

		if (idsToPublish.isEmpty()) {
			return;
		}

		final Multimap<String, String> namespaceToIdsMap = toNamespaceMap(idsToPublish);

		for (Entry<String, Collection<String>> entry : namespaceToIdsMap.asMap().entrySet()) {

			String namespace = entry.getKey();

			for (List<String> ids : partition(newArrayList(entry.getValue()), BULK_LIMIT)) {

				HttpPut bulkRequest = null;
				
				try {

					LOGGER.debug("Sending bulk publish request for namespace {} with size {}", namespace, ids.size());

					bulkRequest = httpPut("sct/bulk/publish", createBulkPublishData(namespace, ids));

					waitForJob(extractJobId(execute(bulkRequest)));

				} catch (IOException e) {
					throw new SnowowlRuntimeException(
							String.format("Unable to bulk publish IDs for namespace %s with size %s", namespace, ids.size()), e);
				} finally {
					release(bulkRequest);
				}
			}
		}
	}

	@Override
	public SctId getSctId(final String componentId) {
		
		HttpGet request = null;
		
		try {
			
			LOGGER.debug("Sending single component ID get request for {}", componentId);
			request = httpGet(String.format("sct/ids/%s", componentId));
			
			return extractSctId(execute(request));
			
		} catch (IOException e) {
			throw new SnowowlRuntimeException(String.format("Unable to get single component ID %s"), e);
		} finally {
			release(request);
		}
		
	}

	@Override
	public Collection<SctId> getSctIds(final Collection<String> componentIds) {
		
		LOGGER.debug("Sending bulk component ID get request.");
		
		return FluentIterable
			.from(partition(newArrayList(componentIds), BULK_GET_LIMIT))
			.transformAndConcat(new Function<Collection<String>, Collection<SctId>>() {
				@Override
				public Collection<SctId> apply(Collection<String> input) {
					HttpGet request = null;
					try {
						request = httpGet("sct/bulk/ids/", String.format("&sctids=%s", Joiner.on(",").join(input)));
						return extractSctIds(execute(request));
					} catch (IOException e) {
						throw new SnowowlRuntimeException("Unable to get bulk component IDs", e);
					} finally {
						release(request);
					}
				}
		}).toList();
	}

	@Override
	public Collection<SctId> getSctIds() {
		
		HttpGet request = null;
		
		try {
			
			LOGGER.debug("Sending component IDs get request.");
	
			request = httpGet(String.format("sct/ids/"));
			final String response = execute(request);
	
			return extractSctIds(response);
			
		} catch (IOException e) {
			throw new SnowowlRuntimeException("Exception while getting all IDs", e);
		} finally {
			release(request);
		}
	}

	@Override
	public boolean importSupported() {
		return false;
	}

	@Override
	public void dispose() {
		if (null != client) {
			client.logout();
			client.close();
			client = null;
		}
	
		disposed = true;
	}

	@Override
	public boolean isDisposed() {
		return disposed;
	}

	public String getToken() {
		return client.getToken();
	}

	private void login() {
		client.login();
	}

	private HttpGet httpGet(final String prefix) {
		return httpGet(prefix, "");
	}
	
	private HttpGet httpGet(final String prefix, final String suffix) {
		return client.httpGet(String.format("%s?token=%s%s", prefix, getToken(), suffix));
	}

	private HttpPost httpPost(final String prefix, final RequestData data) throws IOException {
		return client.httpPost(String.format("%s?token=%s", prefix, getToken()), data);
	}

	private HttpPut httpPut(final String prefix, final RequestData data) throws IOException {
		return client.httpPut(String.format("%s?token=%s", prefix, getToken()), data);
	}

	private String execute(final HttpRequestBase request) throws IOException {
		CisClientException last = null;
		
		for (long attempt = 0; attempt < numberOfReauthTries; attempt++) {
			
			try {
				return client.execute(request);
			} catch (CisClientException e) {
				
				if (e.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
					last = e;
					LOGGER.warn("Unauthorized response from CIS, retrying request ({} attempt(s) left).", numberOfReauthTries - attempt);
					login();
					
					// Update the corresponding query parameter in the request, then retry
					try {

						URI requestUri = request.getURI();
						URI updatedUri = new URIBuilder(requestUri)
								.setParameter("token", getToken())
								.build();
						
						request.setURI(updatedUri);
						request.reset();

					} catch (URISyntaxException se) {
						throw new IOException("Couldn't update authentication token.", se);
					}
					
				} else {
					throw new BadRequestException(e.getReasonPhrase());
				}
			}
		}
		
		// Re-throw the last captured exception otherwise
		throw new BadRequestException(last.getReasonPhrase());
	}

	private void release(final HttpRequestBase request) {
		if (null != request) {
			client.release(request);
		}
	}

	private void waitForJob(final String jobId) {
		
		if (Strings.isNullOrEmpty(jobId)) {
			throw new SnowowlRuntimeException(String.format("Unknown job id: %s", jobId));
		}
		
		HttpGet request = null;

		try {
			
			LOGGER.debug("Polling job with ID: {}", jobId);

			request = httpGet(String.format("bulk/jobs/%s", jobId));

			int pollTry = 0;

			JobStatus status = JobStatus.PENDING;
			
			while (pollTry < numberOfPollTries) {
				
				String response = execute(request);
				status = extractStatus(response);

				if (JobStatus.FINISHED == status) {
					break;
				} else if (JobStatus.ERROR == status) {
					throw new SnowowlRuntimeException(String.format("Job with ID: %s finished with error: %s", jobId, extractLog(response)));
				} else {
					pollTry++;
					Thread.sleep(timeBetweenPollTries);
				}
			}
			
			if (status != JobStatus.FINISHED) {
				throw new SnowowlRuntimeException(String.format("Job with ID: %s did not finish in %s", jobId, getMaxAllowedJobExecutionTime()));
			}
			
		} catch (InterruptedException | IOException e) {
			throw new SnowowlRuntimeException(String.format("Polling of job with ID: %s was interrupted", jobId), e);
		} finally {
			release(request);
		}
	}

	private String getMaxAllowedJobExecutionTime() {
		return TimeUtil.milliToReadableString(numberOfPollTries * timeBetweenPollTries);
	}

	private Collection<String> getComponentIdsFromBulkJob(final String jobId) {
		
		HttpGet recordsRequest = null;
		
		try {
			
			LOGGER.debug("Requesting records for bulk job with ID: {}", jobId);
			
			recordsRequest = httpGet(String.format("bulk/jobs/%s/records", jobId));
			final String recordsResponse = execute(recordsRequest);
			final JsonNode[] records = mapper.readValue(recordsResponse, JsonNode[].class);
			
			return FluentIterable.from(Arrays.asList(records)).transform(new Function<JsonNode, String>() {
				@Override
				public String apply(JsonNode input) {
					return input.get("sctid").asText();
				}
			}).toList();
			
		} catch (IOException e) {
			throw new SnowowlRuntimeException(String.format("Unable to get records for job with ID: %s", jobId), e);
		} finally {
			release(recordsRequest);
		}
		
	}

	private Collection<String> getValidatedIdCollection(Collection<SctId> ids, Predicate<SctId> predicate) {
		return FluentIterable.from(ids).filter(predicate).transform(new Function<SctId, String>() {
			@Override
			public String apply(SctId input) {
				return input.getSctid();
			}
		}).toList();
	}
	
	
	private Collection<String> validateForRegistration(Collection<SctId> ids) {
		return getValidatedIdCollection(ids, new Predicate<SctId>() {
			@Override 
			public boolean apply(SctId input) {
				boolean result = input.matches(IdentifierStatus.AVAILABLE, IdentifierStatus.RESERVED);
				if (!result) {
					LOGGER.warn("Cannot register ID {} as it is already present with status {}", input.getSctid(), input.getStatus());
				}
				return result;
			}
		});
	}
	
	private Collection<String> validateForDeprecation(Collection<SctId> ids) {
		return getValidatedIdCollection(ids, new Predicate<SctId>() {
			@Override 
			public boolean apply(SctId input) {
				if (input.isDeprecated()) {
					LOGGER.warn("Cannot deprecate ID {} as it is already deprecated", input.getSctid());
				}
				return !input.isDeprecated();
			}
		});
	}
	
	private Collection<String> validateForReleasing(Collection<SctId> ids) {
		return getValidatedIdCollection(ids, new Predicate<SctId>() {
			@Override 
			public boolean apply(SctId input) {
				if (input.isAvailable()) {
					LOGGER.warn("Cannot release ID {} as it is already available", input.getSctid());
				}
				return !input.isAvailable();
			}
		});
	}
	
	private Collection<String> validateForPublishing(Collection<SctId> ids) {
		return getValidatedIdCollection(ids, new Predicate<SctId>() {
			@Override 
			public boolean apply(SctId input) {
				if (input.isPublished()) {
					LOGGER.warn("Cannot publish ID {} as it is already published", input.getSctid());
				}
				return !input.isPublished();
			}
		});
	}

	private RequestData createGenerationData(final String namespace, final ComponentCategory category) throws IOException {
		return new GenerationData(selectNamespace(namespace), clientKey, category);
	}

	private RequestData createBulkGenerationData(final String namespace, final ComponentCategory category, final int quantity)
			throws IOException {
		return new BulkGenerationData(selectNamespace(namespace), clientKey, category, quantity);
	}
	
	private RequestData createRegistrationData(final String componentId) throws IOException {
		return new RegistrationData(getNamespace(componentId), clientKey, componentId);
	}

	private RequestData createBulkRegistrationData(final String namespace, final Collection<String> componentIds) throws IOException {

		List<Record> records = FluentIterable.from(componentIds).transform(new Function<String, Record>() {
			@Override
			public Record apply(String input) {
				return new Record(input);
			}
		}).toList();

		return new BulkRegistrationData(namespace, clientKey, records);
	}

	private RequestData createDeprecationData(final String componentId) throws IOException {
		return new DeprecationData(getNamespace(componentId), clientKey, componentId);
	}

	private RequestData createBulkDeprecationData(final String namespace, final Collection<String> componentIds) throws IOException {
		return new BulkDeprecationData(namespace, clientKey, componentIds);
	}

	private RequestData createReservationData(final String namespace, final ComponentCategory category) throws IOException {
		return new ReservationData(namespace, clientKey, getExpirationDate(), category);
	}

	private RequestData createBulkReservationData(final String namespace, final ComponentCategory category, final int quantity)
			throws IOException {
		return new BulkReservationData(namespace, clientKey, getExpirationDate(), category, quantity);
	}

	private String getExpirationDate() {
		final Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, 1);
		final Date expirationDate = calendar.getTime();

		return Dates.formatByGmt(expirationDate, DateFormats.DEFAULT);
	}

	private RequestData createReleaseData(final String componentId) throws IOException {
		return new ReleaseData(getNamespace(componentId), clientKey, componentId);
	}

	private RequestData createBulkReleaseData(final String namespace, final Collection<String> componentIds) throws IOException {
		return new BulkReleaseData(namespace, clientKey, componentIds);
	}

	private RequestData createPublishData(final String componentId) throws IOException {
		return new PublicationData(getNamespace(componentId), clientKey, componentId);
	}

	private RequestData createBulkPublishData(final String namespace, final Collection<String> componentIds) throws IOException {
		return new BulkPublicationData(namespace, clientKey, componentIds);
	}

	private Multimap<String, String> toNamespaceMap(final Collection<String> componentIdsToPublish) {
		return FluentIterable.from(componentIdsToPublish).index(new Function<String, String>() {
			@Override
			public String apply(String id) {
				return getSafeNamespaceDisplayName(getNamespace(id));
			}
		});
	}

	private String getNamespace(final String componentId) {
		return SnomedIdentifiers.create(componentId).getNamespace();
	}

	private String getSafeNamespaceDisplayName(String namespace) {
		return Strings.isNullOrEmpty(namespace) ? INT_NAMESPACE : namespace;
	}

	private String extractLog(String response) {
		try {
			return mapper.readValue(response, JsonNode.class).get("log").asText();
		} catch (IOException e) {
			throw new SnowowlRuntimeException("Unable to extract log of job", e);
		}
	}

	private JobStatus extractStatus(final String response) {
		try {
			int status = mapper.readValue(response, JsonNode.class).get("status").asInt();
			return JobStatus.get(status);
		} catch (IOException | IllegalArgumentException e) {
			throw new SnowowlRuntimeException("Unable to extract status of job", e);
		}
	}

	private SctId extractSctId(final String response) {
		try {
			return mapper.readValue(response, SctId.class);
		} catch (IOException e) {
			throw new SnowowlRuntimeException("Unable to extract SctId from response.", e);
		}
	}
	
	private Collection<SctId> extractSctIds(final String response) {
		try {
			return Arrays.asList(mapper.readValue(response, SctId[].class));
		} catch (IOException e) {
			throw new SnowowlRuntimeException("Unable to extract SctIds from response.", e);
		}
	}

	private String extractJobId(final String response) {
		try {
			return mapper.readValue(response, JsonNode.class).get("id").asText();
		} catch (IOException e) {
			throw new SnowowlRuntimeException("Unable to extract job id from response", e);
		}
	}
}
