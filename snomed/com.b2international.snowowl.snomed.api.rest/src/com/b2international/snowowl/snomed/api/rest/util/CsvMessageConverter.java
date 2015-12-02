package com.b2international.snowowl.snomed.api.rest.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collection;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.b2international.snowowl.core.domain.CollectionResource;
import com.b2international.snowowl.core.exceptions.NotImplementedException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

public class CsvMessageConverter extends AbstractHttpMessageConverter<CollectionResource> {
	public static final MediaType MEDIA_TYPE = new MediaType("text", "csv", Charset.forName("utf-8"));

	public CsvMessageConverter() {
		super(MEDIA_TYPE);
	}

	protected boolean supports(Class<?> clazz) {
		return CollectionResource.class.isAssignableFrom(clazz);
	}

	protected void writeInternal(CollectionResource response, HttpOutputMessage output) throws IOException, HttpMessageNotWritableException {
		final Collection<Object> items = response.getItems();
		if (!items.isEmpty()) {
			output.getHeaders().setContentType(MEDIA_TYPE);
			output.getHeaders().set("Content-Disposition", "attachment");
			try (OutputStream out = output.getBody()) {
				final CsvMapper mapper = new CsvMapper();
				CsvSchema schema = mapper.schemaFor(items.iterator().next().getClass()).withHeader();
				ObjectWriter writer = mapper.writer(schema);
				for (Object item : items) {
					writer.writeValue(out,  item);
				}
			}
		}
	}
	
	@Override
	protected CollectionResource readInternal(
			Class<? extends CollectionResource> arg0, HttpInputMessage arg1)
			throws IOException, HttpMessageNotReadableException {
		throw new NotImplementedException();
	}

}
