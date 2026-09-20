package org.example.zoonoticdiseaseservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.zoonoticdiseaseservice.entity.ZoonoticDisease;
import org.example.zoonoticdiseaseservice.repository.ZoonoticDiseaseRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class AccessControlFilter extends OncePerRequestFilter {

    private final ZoonoticDiseaseRepository repository;

    public AccessControlFilter(ZoonoticDiseaseRepository repository) {
        this.repository = repository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();

        // Allow browser/API access for now.
        // Authorization is enforced when a role is supplied.
        String role = request.getHeader("X-Role");

        if (role == null || role.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        role = role.toUpperCase();

        String path = request.getRequestURI();

        // National users are read-only.
        if (role.equals("NATIONAL")
                && !method.equalsIgnoreCase("GET")) {

            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "National users have read-only access."
            );
            return;
        }

        // Supervisor and admin actions.
        if (path.matches(".*/\\d+/(approve|reject|request-corrections)$")) {

            if (!role.equals("PROVINCIAL_SUPERVISOR")
                    && !role.equals("PROVINCIAL_ADMIN")
                    && !role.equals("NATIONAL")) {

                response.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        "Only authorized supervisors or administrators can perform approval actions."
                );
                return;
            }
        }

        // Check hazard scope when a hazard is supplied.
        String hazard = request.getHeader("X-Hazard");

        if (hazard != null
                && !hazard.isBlank()
                && !hazard.equalsIgnoreCase("ZOONOTIC_DISEASE")) {

            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "This user is not authorized for the zoonotic disease hazard."
            );
            return;
        }

        // Check ward scope for ward recorders.
        if (role.equals("WARD_RECORDER")) {

            String userWard = request.getHeader("X-Ward");

            if (userWard == null || userWard.isBlank()) {

                response.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        "Ward recorder must have a ward scope."
                );
                return;
            }

            // For requests containing an ID, verify the record's ward.
            String[] parts = path.split("/");

            if (parts.length > 0) {

                String lastPart = parts[parts.length - 1];

                if (lastPart.matches("\\d+")) {

                    Long diseaseId = Long.valueOf(lastPart);

                    Optional<ZoonoticDisease> disease =
                            repository.findById(diseaseId);

                    if (disease.isPresent()
                            && !userWard.equalsIgnoreCase(
                            disease.get().getWard())) {

                        response.sendError(
                                HttpServletResponse.SC_FORBIDDEN,
                                "Ward recorder cannot access another ward."
                        );
                        return;
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
