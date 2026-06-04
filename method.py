void shouldReturn409_WhenBorrowLimitExceeded() {
            
                
                Member student = memberRepository.save(new Member("Test Student", "test@test.com", MembershipType.STUDENT));
                Book book1 = bookRepository.save(new Book("111111", "Title 1", "Author 1", 5, Genre.SCIENCE));
                Book book2 = bookRepository.save(new Book("222222", "Title 2", "Author 2", 5, Genre.SCIENCE));
                Book book3 = bookRepository.save(new Book("333333", "Title 3", "Author 3", 5, Genre.SCIENCE));

              
                restTemplate.postForEntity(baseUrl + "/borrows", new BorrowRequest(student.getId(), book1.getId()), Map.class);
                restTemplate.postForEntity(baseUrl + "/borrows", new BorrowRequest(student.getId(), book2.getId()), Map.class);

                
                BorrowRequest limitExceededRequest = new BorrowRequest(student.getId(), book3.getId());
                ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + "/borrows", limitExceededRequest, Map.class);

                
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().get("status")).isEqualTo(409);
            }
