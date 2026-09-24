# dashboard-service

ONE call that aggregates the five hazard feeds into a summary for the frontend.

**Owner:** Oliver (lead) + group integration   |   **Port:** 8088   |   **No database**

## Files in this service (what each one does)

| File | What it does |
|------|--------------|
| `pom.xml` | Boot 4.1.1 web starter only — deliberately no JPA: there is nothing to store. |
| `DashboardServiceApplication.java` | Entry point. |
| `client/HazardDataFetcher.java` | Calls `GET /api/<plural>` (approved feed) of each hazard service; `fetchApproved` degrades a dead service to an empty list, `isUp` powers the health panel. |
| `service/DashboardService.java` | `summary()`: per-hazard approved counts + by-severity counts + 5 latest approved incidents (sorted by occurredAt, newest first). `health()`: UP/DOWN per hazard service. |
| `controller/DashboardController.java` | `GET /api/dashboard/summary`, `GET /api/dashboard/health`. |
| `DashboardServiceTest.java` (4 tests) | counts per hazard + per severity, latest-5 sorted newest first, a dead hazard service degrades instead of breaking, health map reports UP and DOWN correctly. |

## Try it

```bash
curl http://localhost:8088/api/dashboard/summary
curl http://localhost:8088/api/dashboard/health
```

## Integration phase (this is the group's shared service)

Each member plugs their hazard into the dashboard simply by having their service
registered in Eureka — the fetcher already reads all five feeds. If YOUR feed
shows zeros here, the bug is in your service, and this page tells you that in
one look. That is exactly why the guide made the dashboard the integration
checkpoint.

## If the teacher asks

- **Why aggregate instead of letting the frontend call five services?** One
  request, one place to format, and the frontend never needs to know ports or
  service names. It also gives us the health view for free.
- **What if one hazard service is down?** Its feed contributes zero and health
  shows DOWN — the dashboard still renders everything else.
