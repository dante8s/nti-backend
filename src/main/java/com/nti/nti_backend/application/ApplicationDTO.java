public record ApplicationDTO(
        Long id,
        Long applicantId,
        Long callId,
        String callTitle,
        String programName,
        String programType,
        String status,
        String adminComment,
        UUID organizationId,
        String organizationName,
        Long productOwnerId,
        String productOwnerName,
        String formData,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<MemberSnapshotDTO> teamMembers
) {
    public record MemberSnapshotDTO(Long userId, String email, String role) {}
}