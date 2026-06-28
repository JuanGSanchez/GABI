package access.rest.dto;

/**
 * Request body for {@code POST /api/ask}.
 *
 * @param question the natural-language question to answer over the library catalogue
 *
 * @author JuanGS (workstream c — access layer)
 */
public record AskRequest(String question) {}
