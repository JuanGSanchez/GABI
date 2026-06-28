package core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link IdentifierValidator} — the SQL-injection whitelist guard (D-4 fix).
 *
 * <p>Asserts the contract: only {@code [A-Za-z0-9_]}, non-blank, non-null, max 128 chars
 * are accepted. Everything else must throw {@link LibraryException.InvalidIdentifierException}.
 */
class IdentifierValidatorTest {

    // =========================================================================
    // Valid identifiers (must pass)
    // =========================================================================

    @Test
    @DisplayName("Simple alphabetic identifier is accepted")
    void valid_alpha() {
        assertThat(IdentifierValidator.validate("admin", "context")).isEqualTo("admin");
    }

    @Test
    @DisplayName("Alphanumeric identifier is accepted")
    void valid_alphanumeric() {
        assertThat(IdentifierValidator.validate("table1", "ctx")).isEqualTo("table1");
    }

    @Test
    @DisplayName("Identifier with underscores is accepted")
    void valid_underscores() {
        assertThat(IdentifierValidator.validate("my_table_name", "ctx")).isEqualTo("my_table_name");
    }

    @Test
    @DisplayName("Mixed-case identifier is accepted")
    void valid_mixedCase() {
        assertThat(IdentifierValidator.validate("LibraryDB", "ctx")).isEqualTo("LibraryDB");
    }

    @Test
    @DisplayName("Leading/trailing whitespace is trimmed and accepted")
    void valid_trimmed() {
        assertThat(IdentifierValidator.validate("  admin  ", "ctx")).isEqualTo("admin");
    }

    @Test
    @DisplayName("Identifier of exactly 128 characters is accepted")
    void valid_maxLength() {
        String id128 = "a".repeat(128);
        assertThat(IdentifierValidator.validate(id128, "ctx")).isEqualTo(id128);
    }

    // =========================================================================
    // Invalid identifiers (must throw InvalidIdentifierException)
    // =========================================================================

    @Test
    @DisplayName("null identifier throws InvalidIdentifierException")
    void invalid_null() {
        assertThatThrownBy(() -> IdentifierValidator.validate(null, "ctx"))
                .isInstanceOf(LibraryException.InvalidIdentifierException.class)
                .hasMessageContaining("null or blank");
    }

    @Test
    @DisplayName("blank identifier throws InvalidIdentifierException")
    void invalid_blank() {
        assertThatThrownBy(() -> IdentifierValidator.validate("   ", "ctx"))
                .isInstanceOf(LibraryException.InvalidIdentifierException.class)
                .hasMessageContaining("null or blank");
    }

    @Test
    @DisplayName("empty identifier throws InvalidIdentifierException")
    void invalid_empty() {
        assertThatThrownBy(() -> IdentifierValidator.validate("", "ctx"))
                .isInstanceOf(LibraryException.InvalidIdentifierException.class);
    }

    @Test
    @DisplayName("Identifier longer than 128 chars throws InvalidIdentifierException")
    void invalid_tooLong() {
        String id129 = "a".repeat(129);
        assertThatThrownBy(() -> IdentifierValidator.validate(id129, "ctx"))
                .isInstanceOf(LibraryException.InvalidIdentifierException.class)
                .hasMessageContaining("maximum length");
    }

    @ParameterizedTest
    @DisplayName("SQL metacharacters throw InvalidIdentifierException")
    @ValueSource(strings = {
        "user name",         // space
        "user-name",         // hyphen
        "user.name",         // dot
        "user'name",         // single quote
        "user;DROP--",       // semicolon + comment
        "'; DROP TABLE--",   // SQL injection attempt
        "user@domain",       // @
        "user!",             // exclamation
        "user#1",            // hash
        "user$x",            // dollar
        "user%x",            // percent
        "user^x",            // caret
        "user&x",            // ampersand
        "user*x",            // asterisk
        "user(x)",           // parens
        "user+x",            // plus
        "user=x",            // equals
        "user[0]",           // brackets
        "user{x}",           // braces
        "user|x",            // pipe
        "user<x>",           // angle brackets
        "user/x",            // slash
        "user\\x",           // backslash
        "user?x",            // question mark
        "user,x",            // comma
        "user:x",            // colon
    })
    void invalid_specialCharacters(String identifier) {
        assertThatThrownBy(() -> IdentifierValidator.validate(identifier, "ctx"))
                .isInstanceOf(LibraryException.InvalidIdentifierException.class)
                .hasMessageContaining("disallowed characters");
    }

    @Test
    @DisplayName("SQL injection via comment syntax is rejected")
    void invalid_sqlCommentDash() {
        assertThatThrownBy(() -> IdentifierValidator.validate("admin--", "ctx"))
                .isInstanceOf(LibraryException.InvalidIdentifierException.class);
    }

    @Test
    @DisplayName("Semicolon-terminated injection attempt is rejected")
    void invalid_semicolonInjection() {
        assertThatThrownBy(() -> IdentifierValidator.validate("user;DELETE FROM admin.books", "ctx"))
                .isInstanceOf(LibraryException.InvalidIdentifierException.class);
    }

    @Test
    @DisplayName("Exception message includes the context string for diagnostics")
    void exceptionMessageIncludesContext() {
        assertThatThrownBy(() -> IdentifierValidator.validate("bad name", "username"))
                .isInstanceOf(LibraryException.InvalidIdentifierException.class)
                .hasMessageContaining("username");
    }
}
