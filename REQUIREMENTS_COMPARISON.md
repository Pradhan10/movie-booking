# Requirements Analysis: req1 vs req2

## Visual Comparison

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    REQ1: Problem Statement (v1.3.3)                         │
│                        What to Build & Evaluate                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ DEFINES
                                    ▼
        ┌───────────────────────────────────────────────────┐
        │  • Business Problem & Goals                       │
        │  • Functional Features (Read/Write scenarios)     │
        │  • Non-Functional Requirements                    │
        │  • Technology Stack Recommendations               │
        │  • Evaluation Criteria                            │
        └───────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                    REQ2: Implementation Guidelines                          │
│                        How to Present & Deliver                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ SPECIFIES
                                    ▼
        ┌───────────────────────────────────────────────────┐
        │  • Deliverable Artifacts (Diagrams, Code)         │
        │  • User Journey Flows (Success/Failure)           │
        │  • Design Documents (HLD, LLD)                    │
        │  • Implementation Scope (2 Services)              │
        │  • Defense Preparation (Design Justifications)    │
        └───────────────────────────────────────────────────┘
```

---

## Detailed Comparison Table

| Aspect | REQ1 (Problem Statement) | REQ2 (Implementation Guidelines) |
|--------|--------------------------|----------------------------------|
| **Purpose** | Defines WHAT to build | Defines HOW to present it |
| **Scope** | Complete feature set | Subset: Pick 2 services |
| **Focus** | Business requirements | Technical artifacts & justification |
| **Deliverables** | Code + Architecture | Diagrams + Code + GitHub repo |
| **Evaluation** | Breadth of solution | Depth of thinking + defense |

---

## REQ1: Problem Statement Breakdown

### Core Business Problem
Build a **dual-sided platform**:
- **B2B Side**: Theatre partners onboard, manage shows, go digital
- **B2C Side**: Customers browse movies, book tickets

### Functional Features

#### Read Scenarios (Choose 1):
| # | Feature | Description |
|---|---------|-------------|
| R1 | Browse Theatres | Show theatres running selected movie with timings by date |
| R2 | Browse Offers | 50% off 3rd ticket, 20% off afternoon shows |

#### Write Scenarios (Choose 1):
| # | Feature | Description |
|---|---------|-------------|
| W1 | Book Tickets | Select theatre, timing, seats for the day |
| W2 | Show Management | Theatres CRUD shows |
| W3 | Bulk Operations | Bulk booking and cancellation |
| W4 | Inventory Management | Allocate and update seat inventory |

### Non-Functional Requirements
- Transactional integrity
- Theatre system integration (legacy + new)
- Multi-city/country scalability (99.99% availability)
- Payment gateway integration
- Platform monetization strategy
- OWASP Top 10 security
- Compliance requirements

### Technology Recommendations
- **Language**: Java (+ add-ons)
- **Frameworks**: Any
- **AI**: Suggest options
- **Database**: Any
- **Cloud**: Any

### Evaluation Criteria
1. Code artifacts (APIs, patterns, implementation)
2. Design principles (functional + non-functional)
3. DB & data models
4. Platform solutions detailing
5. Completeness, presentation, discussion
6. Uniqueness & extensibility

---

## REQ2: Implementation Guidelines Breakdown

### Deliverable Artifacts

#### 1. User Journey Flows
- **Success scenarios** - Happy path flows
- **Failure scenarios** - Error handling paths
- **Edge case**: Payment success but notification fails → Where to redirect?
- **Tool**: Use draw.io for diagrams

#### 2. Design Documents
- **HLD** (High Level Design) - System architecture
- **LLD** (Low Level Design) - Class diagrams OR Entity diagrams

#### 3. Implementation
- **Scope**: Pick **ANY TWO services** from req1
- **Type**: End-to-end running code
- **No UI required** - Service layer only
- **Deliverable**: GitHub repository link

### Defense Preparation

Be ready to justify:

| Decision Area | Key Questions |
|---------------|---------------|
| **Database** | Why NoSQL vs SQL? Trade-offs? |
| **Caching** | Why Redis vs in-memory? When to use each? |
| **Concurrency** | How to prevent duplicate seat bookings? Optimistic locking? |
| **All Choices** | Why this approach? What alternatives considered? |

### Context
- **Client**: Tesco at Bangalore
- **Assessment Goal**: Evaluate architectural thinking, not just coding ability

---

## Key Differences Highlighted

### REQ1 = The Problem
- Comprehensive feature list
- Choose from multiple scenarios
- Broad evaluation across all areas
- Optional: Skip uncomfortable areas

### REQ2 = The Solution Presentation
- User journey flows mandatory
- Diagrams required (draw.io)
- Only 2 services needed for code
- GitHub repository required
- Deep dive into design rationale
- Expect aggressive questioning on choices

---

## Synthesis: Combined Requirements

To succeed, you must:

1. ✅ **Select 2 scenarios** from req1 (1 read + 1 write recommended)
2. ✅ **Create draw.io flows** showing user journeys with redirects
3. ✅ **Design HLD** showing system components
4. ✅ **Design LLD** with class/entity diagrams
5. ✅ **Implement 2 services** with clean, running code
6. ✅ **Document design decisions** with justifications
7. ✅ **Push to GitHub** for interviewer review
8. ✅ **Prepare defense** for every architectural choice

---

## Recommended Approach

### Two Services to Implement (My Recommendation):

**Service 1: Booking Service** (Write Scenario)
- Handles ticket booking flow
- Demonstrates: Concurrency control, transactions, payment integration
- High complexity, shows technical depth

**Service 2: Show Catalog Service** (Read Scenario)
- Browse theatres and show timings
- Demonstrates: Caching strategy, query optimization, data modeling
- Good balance with Booking Service

### Why These Two?
- Cover both read and write scenarios
- Touch core business value (booking = revenue)
- Allow demonstration of critical technical challenges:
  - Distributed locking (booking conflicts)
  - Caching strategies (catalog browsing)
  - Transaction management (payment + inventory)
  - Event-driven architecture (notifications)

---

## Status
✅ **req1 stored** - Problem statement with full feature set
✅ **req2 stored** - Implementation guidelines and deliverables

**Next Steps**: Proceeding with implementation per your request...
