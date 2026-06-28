package access.rest.dto;

import tables.Member;

/**
 * Data-transfer object for {@link Member}.
 *
 * <p>Omits the internal {@code listBook} field intentionally — the loan list is available
 * through the dedicated {@code /api/members/{id}/loans} endpoint.
 *
 * @param id      unique member identifier
 * @param name    first name
 * @param surname surname / family name
 *
 * @author JuanGS (workstream c — access layer)
 */
public record MemberDto(int id, String name, String surname) {

    /** Maps a domain {@link Member} to its DTO representation. */
    public static MemberDto from(Member m) {
        return new MemberDto(m.getID(), m.getName(), m.getSurname());
    }
}
