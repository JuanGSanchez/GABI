package core;

import core.search.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tables.Member;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.*;

/**
 * SPEC-R06 acceptance over in-memory Derby: paginated, multi-field, case-insensitive,
 * sorted member search using parameterized queries (the sort identifier is whitelisted and
 * validated — no interpolation of user input). Mirrors the SPEC-20 book search tests.
 */
class LibraryServiceImplMembersPagedSearchTest {

    private LibraryServiceImpl service;

    @BeforeEach
    void setUp() throws SQLException {
        DataSource ds = new InMemoryDerbyConfig().testDataSource();
        TestSchemaHelper.createSchema(ds);
        service = new LibraryServiceImpl(ds, new StubRagService());
        service.addMember(1, "Ana", "Garcia");
        service.addMember(2, "Bruno", "Garcia");
        service.addMember(3, "Carla", "Lopez");
        service.addMember(4, "Diego", "Martin");
    }

    @Test
    @DisplayName("matches across both name and surname, case-insensitively")
    void multiFieldCaseInsensitive() {
        Page<Member> bySurname = service.searchMembersPaged("garcia", 0, 10, "id", true);
        assertThat(bySurname.totalElements()).isEqualTo(2);
        Page<Member> byName = service.searchMembersPaged("ANA", 0, 10, "id", true);
        assertThat(byName.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("blank query returns all members with pagination metadata")
    void blankReturnsAll() {
        Page<Member> all = service.searchMembersPaged("", 0, 3, "id", true);
        assertThat(all.totalElements()).isEqualTo(4);
        assertThat(all.content()).hasSize(3);
        assertThat(all.totalPages()).isEqualTo(2);
        assertThat(all.hasNext()).isTrue();
    }

    @Test
    @DisplayName("pages slice the result set")
    void pages() {
        Page<Member> page0 = service.searchMembersPaged("", 0, 2, "id", true);
        Page<Member> page1 = service.searchMembersPaged("", 1, 2, "id", true);
        assertThat(page0.content()).extracting(Member::getID).containsExactly(1, 2);
        assertThat(page1.content()).extracting(Member::getID).containsExactly(3, 4);
    }

    @Test
    @DisplayName("sorting by surname respects direction")
    void sorting() {
        Page<Member> asc = service.searchMembersPaged("", 0, 10, "surname", true);
        Page<Member> desc = service.searchMembersPaged("", 0, 10, "surname", false);
        assertThat(asc.content().get(0).getSurname()).isEqualTo("Garcia");
        assertThat(desc.content().get(0).getSurname()).isEqualTo("Martin");
    }

    @Test
    @DisplayName("sorting by name respects direction")
    void sortingByName() {
        Page<Member> asc = service.searchMembersPaged("", 0, 10, "name", true);
        assertThat(asc.content().get(0).getName()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("an unknown sort field falls back to a safe default (no injection)")
    void unknownSortDefaults() {
        Page<Member> page = service.searchMembersPaged("", 0, 10, "name; DROP TABLE admin.members", true);
        assertThat(page.totalElements()).isEqualTo(4);
    }
}
