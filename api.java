
    @Nested
    @DisplayName("POST /api/borrows - Error cases")
    class BorrowErrorsApi {

        @Test
        @DisplayName("should return 409 when borrowing limit exceeded")
        void shouldReturn409_WhenBorrowLimitExceeded() {
           
            Member student = createTestMember("B", "b@test.com", MembershipType.STUDENT);
            Book book1 = createTestBook("978-1", "Book One", "Author A");
            Book book2 = createTestBook("978-2", "Book Two", "Author B");
            Book book3 = createTestBook("978-3", "Book Three", "Author C");

            restTemplate.postForEntity(baseUrl + "/borrows",
                    new BorrowRequest(book1.getId(), student.getId()), Map.class);
            restTemplate.postForEntity(baseUrl + "/borrows",
                    new BorrowRequest(book2.getId(), student.getId()), Map.class);

         
            ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + "/borrows",
                    new BorrowRequest(book3.getId(), student.getId()), Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).containsKey("message");
        }

        @Test
        @DisplayName("should return 409 when no copies available")
        void shouldReturn409_WhenNoCopiesAvailable() {
  
            Book singleCopyBook = createTestBook("978-1", "Rare Book", "Author A", 1);
            Member member1 = createTestMember("Ece", "ece@test.com", MembershipType.STANDARD);
            Member member2 = createTestMember("Bob", "bob@test.com", MembershipType.STANDARD);

       
            ResponseEntity<Map> first = restTemplate.postForEntity(baseUrl + "/borrows",
                    new BorrowRequest(singleCopyBook.getId(), member1.getId()), Map.class);
            assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            ResponseEntity<Map> second = restTemplate.postForEntity(baseUrl + "/borrows",
                    new BorrowRequest(singleCopyBook.getId(), member2.getId()), Map.class);

            assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(second.getBody()).containsKey("message");
        }

        @Test
        @DisplayName("should return 404 when member does not exist")
        void shouldReturn404_WhenMemberNotFound() {
            Book book = createTestBook("978-1", "Test Book", "Author A");

            ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + "/borrows",
                    new BorrowRequest(book.getId(), 99999L), Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).containsKey("message");
        }

        @Test
        @DisplayName("should return 404 when book does not exist")
        void shouldReturn404_WhenBookNotFound() {
            Member member = createTestMember("Ece", "ece@test.com", MembershipType.STANDARD);

            ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + "/borrows",
                    new BorrowRequest(99999L, member.getId()), Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).containsKey("message");
        }
    }
