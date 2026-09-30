package com.sonrisa.alerting.app.persistence.subscriber;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Subscribers (Section 8.1). The unique fingerprint enforces one subscription per address (FR-05). */
public interface SubscriberRepository extends JpaRepository<Subscriber, UUID> {

    /** Admin list order (FR-25): newest subscription first; the id makes the order stable across pages. */
    Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("subscribedAt"), Sort.Order.desc("id"));

    boolean existsByAddressFingerprint(String addressFingerprint);

    Optional<Subscriber> findByAddressFingerprint(String addressFingerprint);

    /**
     * Admin search (FR-26): subscribers whose name or address contains {@code term}, ignoring case. Encrypted
     * addresses (webhook URLs) are not searched. A blank term lists all subscribers. Paging and order come from
     * {@code pageable}, for example with {@link #NEWEST_FIRST}.
     */
    default Page<Subscriber> search(String term, Pageable pageable) {
        if (term == null || term.isBlank()) {
            return findAll(pageable);
        }
        return findByLowerCasePattern(containsPattern(term), pageable);
    }

    /** Use {@link #search(String, Pageable)}; the pattern must be lower case with {@code \} as escape character. */
    @Query("""
            select s from Subscriber s
            where lower(s.displayName) like :pattern escape '\\'
               or (s.addressEncrypted = false and lower(s.storedAddress) like :pattern escape '\\')
            """)
    Page<Subscriber> findByLowerCasePattern(@Param("pattern") String pattern, Pageable pageable);

    /** A LIKE pattern that matches {@code term} literally anywhere, so {@code %} and {@code _} lose their meaning. */
    static String containsPattern(String term) {
        String escaped = term.strip().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
