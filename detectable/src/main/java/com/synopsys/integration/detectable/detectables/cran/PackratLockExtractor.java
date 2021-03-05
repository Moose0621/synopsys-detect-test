/*
 * detectable
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detectable.detectables.cran;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.bdio.graph.DependencyGraph;
import com.synopsys.integration.detectable.extraction.Extraction;
import com.synopsys.integration.detectable.detectable.codelocation.CodeLocation;
import com.synopsys.integration.common.util.finder.FileFinder;
import com.synopsys.integration.detectable.detectables.cran.parse.PackratDescriptionFileParser;
import com.synopsys.integration.detectable.detectables.cran.parse.PackratLockFileParser;
import com.synopsys.integration.util.NameVersion;

public class PackratLockExtractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final PackratDescriptionFileParser packratDescriptionFileParser;
    private final PackratLockFileParser packRatLockFileParser;
    private final FileFinder fileFinder;

    public PackratLockExtractor(final PackratDescriptionFileParser packratDescriptionFileParser, final PackratLockFileParser packRatLockFileParser, final FileFinder fileFinder) {
        this.packratDescriptionFileParser = packratDescriptionFileParser;
        this.packRatLockFileParser = packRatLockFileParser;
        this.fileFinder = fileFinder;
    }

    public Extraction extract(final File directory, final File packratlock) {
        try {
            final NameVersion nameVersion = determineProjectNameVersion(directory);

            final List<String> packratLockText = Files.readAllLines(packratlock.toPath(), StandardCharsets.UTF_8);
            final DependencyGraph dependencyGraph = packRatLockFileParser.parseProjectDependencies(packratLockText);
            final CodeLocation codeLocation = new CodeLocation(dependencyGraph);

            return new Extraction.Builder().success(codeLocation).projectName(nameVersion.getName()).projectVersion(nameVersion.getVersion()).build();
        } catch (final Exception e) {
            return new Extraction.Builder().exception(e).build();
        }
    }

    private NameVersion determineProjectNameVersion(final File directory) throws IOException {
        final File descriptionFile = fileFinder.findFile(directory, "DESCRIPTION");
        final String defaultProjectName = "";
        final String defaultProjectVersion = "";
        final NameVersion nameVersion;

        if (descriptionFile != null) {
            final List<String> descriptionFileLines = Files.readAllLines(descriptionFile.toPath(), StandardCharsets.UTF_8);
            logger.debug(String.join(System.lineSeparator(), descriptionFileLines));

            nameVersion = packratDescriptionFileParser.getProjectNameVersion(descriptionFileLines, defaultProjectName, defaultProjectVersion);
        } else {
            nameVersion = new NameVersion(defaultProjectName, defaultProjectVersion);
        }

        return nameVersion;
    }

}
