package pl.touk.sputnik.processor.sonar;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import pl.touk.sputnik.review.ReviewResult;
import pl.touk.sputnik.review.Severity;

import com.google.common.io.Resources;

public class SonarResultParserTest {

    @Test
    public void testParseJson() throws IOException {
        File resultFile = new File(Resources.getResource("json/sonar-result.json").getFile());

        SonarResultParser parser = new SonarResultParser(resultFile);
        ReviewResult reviewResult = parser.parseResults();

        assertThat(reviewResult).isNotNull();
        assertThat(reviewResult.getViolations()).hasSize(4);
        assertThat(reviewResult.getViolations())
                .extracting("filenameOrJavaClassName")
                .containsExactly(
                        "src/module1/dir/file.cs",
                        "src/module2/dir/file2.cs",
                        "src/module2/dir/file2.cs",
                        "src/module2/dir/file2.cs"
                );
        assertThat(reviewResult.getViolations())
        .extracting("severity")
        .containsExactly(
                Severity.WARNING,
                Severity.ERROR,
                Severity.ERROR,
                Severity.ERROR
        );
    }

    @Test(expected=IOException.class)
    public void testWithNonExistingFile() throws IOException {
        new SonarResultParser(new File("foo")).parseResults();
    }

    @Test(expected=IOException.class)
    public void testWithInvalidFile() throws IOException {
        new SonarResultParser(new File(Resources.getResource("json/invalid.json").getFile())).parseResults();
    }

    @Test
    public void testConvertSeverity() {
        assertThat(SonarResultParser.getSeverity("BLOCKER")).isEqualTo(Severity.ERROR);
        assertThat(SonarResultParser.getSeverity("CRITICAL")).isEqualTo(Severity.ERROR);
        assertThat(SonarResultParser.getSeverity("MAJOR")).isEqualTo(Severity.ERROR);
        assertThat(SonarResultParser.getSeverity("MINOR")).isEqualTo(Severity.WARNING);
        assertThat(SonarResultParser.getSeverity("INFO")).isEqualTo(Severity.INFO);
        assertThat(SonarResultParser.getSeverity("dummy")).isEqualTo(Severity.WARNING);
    }
}