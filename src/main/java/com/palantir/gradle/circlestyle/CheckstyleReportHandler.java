/*
 * Copyright 2017 Palantir Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.gradle.circlestyle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

class CheckstyleReportHandler extends DefaultHandler {

    public static ReportParser PARSER = new ReportParser() {
        @Override
        public List<Failure> loadFailures(InputStream report) {
            try {
                CheckstyleReportHandler handler = new CheckstyleReportHandler();
                XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
                xmlReader.setContentHandler(handler);
                xmlReader.parse(new InputSource(report));
                return handler.failures();
            } catch (SAXException | ParserConfigurationException | IOException e) {
                throw new AssertionError(e);
            }
        }
    };

    private final List<Failure> failures = new ArrayList<>();
    private File file;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        switch (qName) {
            case "file":
                file = new File(attributes.getValue("name"));
                break;

            case "error":
                failures.add(new Failure.Builder()
                        .source(attributes.getValue("source"))
                        .severity(attributes.getValue("severity").toUpperCase())
                        .file(file)
                        .line(Integer.parseInt(attributes.getValue("line")))
                        .message(attributes.getValue("message"))
                        .build());

            default:
                break;
        }
    }

    List<Failure> failures() {
        return failures;
    }
}
