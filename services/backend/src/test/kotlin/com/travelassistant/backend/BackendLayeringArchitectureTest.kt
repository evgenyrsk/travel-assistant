package com.travelassistant.backend

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.test.Test
import kotlin.test.assertTrue

class BackendLayeringArchitectureTest {

    @Test
    fun domainDoesNotDependOnOuterLayersOrKtor() {
        assertNoForbiddenImports(
            layer = "domain",
            forbiddenPrefixes = setOf(
                "com.travelassistant.backend.api",
                "com.travelassistant.backend.application",
                "com.travelassistant.backend.infrastructure",
                "io.ktor",
            ),
        )
    }

    @Test
    fun applicationDoesNotDependOnApiInfrastructureOrKtor() {
        assertNoForbiddenImports(
            layer = "application",
            forbiddenPrefixes = setOf(
                "com.travelassistant.backend.api",
                "com.travelassistant.backend.infrastructure",
                "io.ktor",
            ),
        )
    }

    @Test
    fun apiDoesNotCallInfrastructureDirectly() {
        assertNoForbiddenImports(
            layer = "api",
            forbiddenPrefixes = setOf("com.travelassistant.backend.infrastructure"),
        )
    }

    private fun assertNoForbiddenImports(
        layer: String,
        forbiddenPrefixes: Set<String>,
    ) {
        val violations = sourceFiles(layer).flatMap { sourceFile ->
            Files.readAllLines(sourceFile)
                .mapNotNull(IMPORT_PATTERN::matchEntire)
                .map { match -> match.groupValues[1] }
                .filter { importedName ->
                    forbiddenPrefixes.any { prefix ->
                        importedName == prefix || importedName.startsWith("$prefix.")
                    }
                }
                .map { importedName -> "$sourceFile imports $importedName" }
        }

        assertTrue(
            violations.isEmpty(),
            violations.joinToString(
                prefix = "Layer '$layer' has forbidden dependencies:\n",
                separator = "\n",
            ),
        )
    }

    private fun sourceFiles(layer: String): List<Path> {
        val layerDirectory = MAIN_SOURCE_ROOT.resolve(layer)
        assertTrue(Files.isDirectory(layerDirectory), "Missing source layer: $layerDirectory")

        return Files.walk(layerDirectory).use { paths ->
            paths
                .filter(Files::isRegularFile)
                .filter { path -> path.extension == "kt" }
                .sorted()
                .toList()
        }
    }

    private companion object {
        val MAIN_SOURCE_ROOT: Path =
            Path.of("src/main/kotlin/com/travelassistant/backend")
        val IMPORT_PATTERN = Regex("\\s*import\\s+([A-Za-z0-9_.]+)(?:\\s+as\\s+\\w+)?\\s*")
    }
}
