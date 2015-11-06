/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.ingest.processor.date;

import org.elasticsearch.ingest.Data;
import org.elasticsearch.ingest.processor.Processor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DateProcessor implements Processor {

    public static final String TYPE = "date";
    static final String DEFAULT_TARGET_FIELD = "@timestamp";

    private final DateTimeZone timezone;
    private final Locale locale;
    private final String matchField;
    private final String targetField;
    private final List<DateParser> dateParsers;

    DateProcessor(DateTimeZone timezone, Locale locale, String matchField, List<String> matchFormats, String targetField) {
        this.timezone = timezone;
        this.locale = locale;
        this.matchField = matchField;
        this.targetField = targetField;
        this.dateParsers = new ArrayList<>();
        for (String matchFormat : matchFormats) {
             dateParsers.add(DateParserFactory.createDateParser(matchFormat, timezone, locale));
        }
    }

    @Override
    public void execute(Data data) {
        String value = data.getProperty(matchField);
        // TODO(talevy): handle custom timestamp fields

        DateTime dateTime = null;
        Exception lastException = null;
        for (DateParser dateParser : dateParsers) {
            try {
                dateTime = dateParser.parseDateTime(value);
            } catch(Exception e) {
                //TODO is there a better way other than catching exception?
                //try the next parser
                lastException = e;
            }
        }

        if (dateTime == null) {
            throw new IllegalArgumentException("unable to parse date [" + value + "]", lastException);
        }

        String dateAsISO8601 = dateTime.toString();
        data.addField(targetField, dateAsISO8601);
    }

    public static class Factory implements Processor.Factory {

        @SuppressWarnings("unchecked")
        public Processor create(Map<String, Object> config) {
            String timezoneString = (String) config.get("timezone");
            DateTimeZone timezone = (timezoneString == null) ? DateTimeZone.UTC : DateTimeZone.forID(timezoneString);
            String localeString = (String) config.get("locale");
            Locale locale = localeString == null ? Locale.ENGLISH : Locale.forLanguageTag(localeString);
            String matchField = (String) config.get("match_field");
            List<String> matchFormats = (List<String>) config.get("match_formats");
            String targetField = (String) config.get("target_field");
            if (targetField == null) {
                targetField = DEFAULT_TARGET_FIELD;
            }
            return new DateProcessor(timezone, locale, matchField, matchFormats, targetField);
        }
    }

}
