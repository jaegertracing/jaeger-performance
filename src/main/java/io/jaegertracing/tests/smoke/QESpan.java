/**
 * Copyright ${license.git.copyrightYears} The Jaeger Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.jaegertracing.tests.smoke;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import io.opentracing.Span;
import io.opentracing.SpanContext;

public class QESpan implements Span {
    private Map<String, Object> tags = new HashMap<String, Object>();
    private Long start;
    private Long duration;
    private String operation;
    private String id;
    private QESpan parent;
    private Span span;
    private JsonNode json;

    private QESpan(Builder builder) {
        this.tags = builder.tags;
        this.start = builder.start;
        this.duration = builder.duration;
        this.operation = builder.operation;
        this.id = builder.id;
        this.parent = builder.parent;
        this.span = builder.span;
        this.json = builder.json;
    }

    public static class Builder {
        // Required parameters
        private Map<String, Object> tags = new HashMap<String, Object>();
        private Long start;
        private Long duration;
        private String operation;
        private String id;

        // Optional
        private Span span;
        private QESpan parent;
        private JsonNode json;

        public Builder(Map<String, Object> tags, Long start, Long duration, String operation, String id) {
            this.tags = tags;
            this.start = start;
            this.duration = duration;
            this.operation = operation;
            this.id = id;
        }

        public Builder parent(QESpan parent) {
            this.parent = parent;
            return this;
        }

        public Builder span(Span span) {
            this.span = span;
            return this;
        }

        public Builder json(JsonNode json) {
            this.json = json;
            return this;
        }

        public QESpan build() {
            return new QESpan(this);
        }

    }

    public Span setOperationName(String operation) {
        this.operation = operation;
        return this;
    }

    public Span setTag(String name, String value) {
        this.tags.put(name, value);
        return this;
    }

    public Span setTag(String name, boolean value) {
        this.tags.put(name, value);
        return this;
    }

    public Span setTag(String name, Number value) {
        this.tags.put(name, value);
        return this;
    }

    @Override
    public Span log(Map<String, ?> fields) {
        span.log(fields);
        return this;
    }

    @Override
    public Span log(long timestampMicroseconds, Map<String, ?> fields) {
        span.log(timestampMicroseconds, fields);
        return this;
    }

    @Override
    public Span log(String event) {
        span.log(event);
        return this;
    }

    @Override
    public Span log(long timestampMicroseconds, String event) {
        span.log(timestampMicroseconds, event);
        return this;
    }

    @Override
    public Span setBaggageItem(String key, String value) {
        span.setBaggageItem(key, value);
        return this;
    }

    @Override
    public String getBaggageItem(String key) {
        return span.getBaggageItem(key);
    }

    @Override
    public void finish(long end) {
        if (span != null) {
            span.finish(end);
        }
    }

    @Override
    public void finish() {
        finish(TimeUnit.MILLISECONDS.toMicros(Instant.now().toEpochMilli()));
    }

    @Override
    public SpanContext context() {
        return span.context();
    }

    public Long getDuration() {
        return duration;
    }

    public JsonNode getJson() {
        return json;
    }

    public Map<String, Object> getTags() {
        return tags;
    }

    public Long getStart() {
        return start;
    }

    public String getOperation() {
        return operation;
    }

    public String getId() {
        return id;
    }

    public QESpan getParent() {
        return parent;
    }

    public Span getspan() {
        return span;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !QESpan.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        final QESpan other = (QESpan) obj;
        if (!getOperation().equals(other.getOperation())) {
            return false;
        }
        if (getStart().compareTo(other.getStart()) != 0) {
            return false;
        }
        if (getDuration().compareTo(other.getDuration()) != 0) {
            return false;
        }
        if (!getTags().keySet().equals(other.getTags().keySet())) {
            return false;
        }
        for (String name : getTags().keySet()) {
            if (getTags().get(name) instanceof Number) {
                if (!getTags().get(name).toString().equals(other.getTags().get(name).toString())) {
                    return false;
                }
            } else if (tags.get(name) instanceof Boolean) {
                if (getTags().get(name) != other.getTags().get(name)) {
                    return false;
                }
            } else {
                if (!getTags().get(name).equals(other.getTags().get(name))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + operation.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "QESpan{" +
                "tags=" + tags +
                ", start=" + start +
                ", duration=" + duration +
                ", operation='" + operation + '\'' +
                ", id='" + id + '\'' +
                ", parent=" + parent +
                ", span=" + span +
                ", json=" + json +
                '}';
    }
}