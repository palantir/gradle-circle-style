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

class FindBugsReportHandler extends DefaultHandler {

    public static final ReportParser PARSER = new ReportParser() {
        @Override
        public List<Failure> loadFailures(InputStream report) {
            try {
                FindBugsReportHandler handler = new FindBugsReportHandler();
                XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
                xmlReader.setContentHandler(handler);
                xmlReader.parse(new InputSource(report));
                return handler.failures();
            } catch (SAXException | ParserConfigurationException | IOException e) {
                throw new AssertionError(e);
            }
        }
    };

    private final List<String> sources = new ArrayList<>();
    private final List<Failure> failures = new ArrayList<>();
    private Failure.Builder failure = null;
    private StringBuilder content = null;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        switch (qName) {
            case "SrcDir":
                content = new StringBuilder();
                break;

            case "BugInstance":
                failure = new Failure.Builder()
                        .source(attributes.getValue("type"))
                        .severity("ERROR");
                break;

            case "LongMessage":
                content = new StringBuilder();

            case "SourceLine":
                if ("true".equals(attributes.getValue("primary"))) {
                    String sourcepath = attributes.getValue("sourcepath");
                    File sourceFile = new File(sourcepath);
                    for (String source : sources) {
                        if (source.endsWith(sourcepath)) {
                            sourceFile = new File(source);
                        }
                    }
                    failure.file(sourceFile)
                            .line(Integer.parseInt(attributes.getValue("start")));
                }
                break;

            default:
                break;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (content != null) {
            content.append(ch, start, length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        switch (qName) {
            case "SrcDir":
                sources.add(content.toString());
                content = null;
                break;

            case "LongMessage":
                failure.message(content.toString());
                content = null;
                break;

            case "BugInstance":
                failures.add(failure.build());
                failure = null;
                break;

            default:
                break;
        }
    }

    List<Failure> failures() {
        return failures;
    }
}