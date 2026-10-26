## Report details

| Field | Value |
| --- | --- |
| Student | Diara Kegenbay |
| Group | IT-2409 |
| Individual variant | 9 — a named entity graph used by two repository methods |
| Project | spring-lab-01 |
| Repository | https://github.com/user40230/spring-lab-01 |
| Required branch | lab08 |
| Branch URL after publication | https://github.com/user40230/spring-lab-01/tree/lab08 |

### Evidence and environment

All 17 screenshot files supplied for this report are included below in the order of the laboratory tasks. They show the local execution in IntelliJ IDEA, pgAdmin 4 and the browser, using PostgreSQL 18 and the springlab database. One repository screenshot is also repeated under variant 9 so that its implementation can be explained without returning to an earlier section.

SQL statement counts, N+1 measurements and the Page/Slice comparison come from a separately recorded verification run of the same project version with PostgreSQL 16. Local console screenshots of these measurements were not supplied. These results are identified separately and are not attributed to screenshots of source code.

## Task 1. PostgreSQL and the first entity

### 1.1. Prepare the database and branch

Step 1. Create the springlab user. Step 2. Create the springlab database owned by that user. The two SQL commands are executed separately because CREATE DATABASE cannot run inside a transaction block.

```sql
CREATE USER springlab WITH PASSWORD 'springlab';
```

```sql
CREATE DATABASE springlab OWNER springlab;
```

[Screenshot included in the separate Word/PDF report.]

Figure 1. Creating the springlab database: successful CREATE DATABASE result.

The selected database creation command returned CREATE DATABASE in the Messages panel. The project is opened on the local lab08 branch, which is visible in the IDEA screenshots.

### 1.2. Add the dependencies

The project includes spring-boot-starter-data-jpa and the PostgreSQL JDBC driver with runtime scope. Spring Boot manages their versions. The existing web application dependencies are retained.

[Screenshot included in the separate Word/PDF report.]

Figure 2. Spring Data JPA and PostgreSQL dependencies in pom.xml.

### 1.3. Configure the datasource

application.yml contains the connection to localhost:5432/springlab with username and password springlab. open-in-view=false prevents a persistence context from remaining open throughout the web request. create-drop creates the schema at application startup and drops it at shutdown; show-sql and format_sql print readable SQL.

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/springlab
    username: springlab
    password: springlab
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate.format_sql: true
```

[Screenshot included in the separate Word/PDF report.]

Figure 3. Datasource and Hibernate settings in application.yml; the compiled classes copy is open.

The screenshot shows the application.yml copy in the build output directory classes. Its source file is src/main/resources/application.yml. A dedicated laboratory database is used: create-drop is a development setting and does not preserve application data after shutdown.

### 1.4. Convert Book into an entity

Book is a non-final class annotated with @Entity and has a protected no-argument constructor required by JPA. @Id and GenerationType.IDENTITY define a database-generated identifier. Genre is stored by name through EnumType.STRING. createdAt records creation time; @Version supports optimistic locking for ordinary entity updates. The entity contains title, genre, year, archived, author and tags. The complete listing is included in Appendix A.

[Screenshot included in the separate Word/PDF report.]

Figure 4. The Book entity: identity, string enum, creation timestamp, version and associations.

The associations visible in this screenshot are explained in Task 2. The named graph is discussed under individual variant 9.

### 1.5. Replace in-memory storage

BookRepository extends JpaRepository<Book, Long>. Spring Data supplies the CRUD implementation, and the service reads and saves books in PostgreSQL instead of a collection in memory. Task 2.3 demonstrates actual database rows; Task 4 demonstrates that /api/books responds.

## Task 2. Associations

### 2.1. Add Author and Tag and map the associations

Author contains id, name and country. Its books collection uses @OneToMany(mappedBy="author", cascade=ALL, orphanRemoval=true). Book contains @ManyToOne(fetch=LAZY) and @JoinColumn(name="author_id"). Book owns the association because book.author_id holds the foreign key. Author.books is the inverse side and does not create a books column.

```java
public void addBook(Book book) {
    books.add(book);
    book.setAuthor(this);
}
```

The helper adds a book to the author's collection and sets its author on the owning side. Updating both sides keeps the Java association consistent with the foreign key written by JPA.

[Screenshot included in the separate Word/PDF report.]

Figure 5. The Author entity: the inverse OneToMany association and addBook helper.

Tag contains id and name. Book.tags uses @ManyToMany and @JoinTable(name="book_tag"). The join table contains book_id and tag_id referencing book.id and tag.id. Book also owns this association because it declares @JoinTable.

[Screenshot included in the separate Word/PDF report.]

Figure 6. The Tag entity: generated identity and unique name.

### Entity diagram and ownership

Author (1) — (N) Book (N) — (N) Tag, through book_tag. Book owns author_id and book_tag.

Author has a one-to-many relationship with Book. Book and Tag have a many-to-many relationship represented by book_tag. Book owns author_id and the join table; Author.books is the inverse side identified by mappedBy="author".

### 2.2. Seed the database

DevelopmentData implements CommandLineRunner and runs in the dev profile. Inside a transaction it creates four tags and three authors with eight books. Books are linked through addBook. Isaac Asimov owns three books, and I, Robot has the classic and fiction tags.

[Screenshot included in the separate Word/PDF report.]

Figure 7. DevelopmentData: three authors, eight books and four tags in the dev profile.

| Author | Country | Number of seeded books |
| --- | --- | --- |
| Isaac Asimov | USA | 3 |
| Yuval Noah Harari | Israel | 2 |
| J. K. Rowling | United Kingdom | 3 |

### 2.3. Inspect the schema and data in pgAdmin

Step 1. Inspect the public schema tables in Object Explorer.

[Screenshot included in the separate Word/PDF report.]

Figure 8. The author, book, book_tag and tag tables in Object Explorer.

The tree lists the application tables. The following executed queries use the springlab/postgres@PostgreSQL 18 connection.

Step 2. Join books and authors. The result panel reports Total rows: 8.

```sql
SELECT b.id, b.title, b.genre, b.year,
       a.name AS author, a.country
FROM book b
JOIN author a ON a.id = b.author_id
ORDER BY b.id;
```

[Screenshot included in the separate Word/PDF report.]

Figure 9. Eight books and their authors, obtained through a JOIN in springlab.

Step 3. Query book–tag links. There are 11 rows, including two links for I, Robot.

```sql
SELECT b.title, t.name AS tag
FROM book_tag bt
JOIN book b ON b.id = bt.book_id
JOIN tag t ON t.id = bt.tag_id
ORDER BY b.id, t.name;
```

[Screenshot included in the separate Word/PDF report.]

Figure 10. Book–tag associations: 11 rows, including two tags for I, Robot.

Step 4. Query information_schema.columns. The result contains 13 columns across three tables. The visible rows show author.id, author.country, author.name and book.author_id. Some lower rows are outside the screenshot; the book_tag column names are additionally confirmed by the next foreign-key query.

[Screenshot included in the separate Word/PDF report.]

Figure 11. Columns of author, book and book_tag returned by information_schema.columns.

Step 5. Query pg_constraint. The result contains three foreign keys: one linking a book to its author and two linking the join table to books and tags.

[Screenshot included in the separate Word/PDF report.]

Figure 12. Three foreign keys linking books, authors and tags.

Completed table required by Task 2.3:

| Question | Answer | How it was checked |
| --- | --- | --- |
| Which tables were created? | author, book, tag, book_tag | Object Explorer and queries in springlab |
| Where is the author foreign key? | book.author_id references author.id | pg_constraint and the book–author JOIN |
| What columns does book_tag have? | book_id and tag_id | Its two foreign keys and the book–tag JOIN |
| Does author have a books column? | No: its columns are id, country and name | All three author rows in information_schema.columns are visible |

## Task 3. Queries

### 3.1. Five derived queries

BookRepository declares methods to find by genre, search a title fragment ignoring case, filter a year range with descending sorting, obtain the three newest books in a genre, and count books by author name. Spring Data derives queries from these method names.

[Screenshot included in the separate Word/PDF report.]

Figure 13. BookRepository: derived queries, JPQL, bulk update, fetch join and entity graphs.

### 3.2. JPQL and a bulk update

The repository screenshot also shows two methods annotated with @Query:

```java
@Query("""
    select b from Book b
    where b.genre = :genre and b.year >= :year
    order by b.title
    """)
List<Book> search(@Param("genre") Genre genre, @Param("year") int year);

@Modifying(clearAutomatically = true)
@Query("update Book b set b.archived = true where b.year < :year")
int archiveOlderThan(@Param("year") int year);
```

search uses named parameters. archiveOlderThan sets archived=true in bulk rather than deleting records. Its caller in BookService is annotated with @Transactional. clearAutomatically clears the persistence context after the bulk update so that stale managed objects are not reused.

### 3.3. Execute the methods and record the results

The following table is filled from the recorded verification run of this project version (Lab08Tests, PostgreSQL 16). The local screenshot demonstrates the declarations; execution results and SQL counters are supported separately by the verification log. The seed data matches the local evidence: eight books and three authors.

| Method and arguments | Rows returned or updated | SQL statements |
| --- | --- | --- |
| findByGenre(SCIENCE) | 3 books | 1 |
| findByTitleContainingIgnoreCase("HARRY") | 3 books | 1 |
| findTop3ByGenreOrderByYearDesc(SCIENCE) | 3 books | 1 |
| search(SCIENCE,1951), JPQL | 2 books | 1 |
| archiveOlderThan(1960), bulk | 3 updated rows | 1 |
| findByYearBetweenOrderByYearDesc(1950,1954) | 3 books | 1 |
| countByAuthorName("Isaac Asimov") | Count value: 3 | 1 |

Hibernate SQL for a derived query, recorded during verification:

```sql
select
        b1_0.id,
        b1_0.archived,
        a1_0.id,
        a1_0.country,
        a1_0.name,
        b1_0.created_at,
        b1_0.genre,
        b1_0.title,
        b1_0.version,
        b1_0.year 
    from
        book b1_0 
    left join
        author a1_0 
            on a1_0.id=b1_0.author_id 
    where
        upper(b1_0.title) like upper(?) escape '\'
```

findByTitleContainingIgnoreCase("HARRY") binds the pattern %HARRY%. The named graph fetches the author in the same SQL statement.

### 3.4. Measure and resolve N+1

First, all books were loaded in a fresh persistence context and each author's name was printed. The context was then cleared and the traversal was repeated using findAllWithAuthor:

```java
@Query("select b from Book b join fetch b.author")
List<Book> findAllWithAuthor();
```

| Verification scenario | Books | SQL statements |
| --- | --- | --- |
| Plain loading followed by reading author names | 8 | 4 |
| Fetch join followed by reading author names | 8 | 1 |

Four statements mean one query for books and three queries for distinct authors. A repeated author is reused from the persistence context. The fetch-join declaration is visible in Figure 13; the measured counts come from the saved test log, not from the code screenshot.

## Task 4. Pagination and sorting

### 4.1. Return a page from the collection endpoint

GET /api/books returns Page<BookResponse>. Spring MVC builds Pageable from page, size and sort. Defaults are size=5 and sorting by title. The repository contains Page<Book> findByGenre(Genre genre, Pageable pageable). Response DTOs prevent direct JSON serialization of JPA entities and their lazy associations.

### 4.2. Check the three required requests

Step 1. Request GET /api/books?page=0&size=3. The initial response is shown before formatting, followed by the same response with automatic JSON formatting enabled. It contains three books, number=0, totalElements=8 and totalPages=3. Title sorting is applied by default.

[Screenshot included in the separate Word/PDF report.]

Figure 14. The first API page before automatic JSON formatting: page=0 and size=3.

[Screenshot included in the separate Word/PDF report.]

Figure 15. The same first API page after formatting: three books, totalElements=8 and totalPages=3.

Step 2. Request GET /api/books?page=1&size=3&sort=year,desc. The second page contains books from 1998, 1997 and 1954. Page numbering starts at zero.

[Screenshot included in the separate Word/PDF report.]

Figure 16. The second API page sorted by year descending: 1998, 1997 and 1954.

Readable excerpt of the second-page response: only book identifiers, titles, years and pagination fields are reproduced below; other response fields are omitted for brevity.

```json
{
  "content": [
    {"id": 7, "title": "Harry Potter and the Chamber of Secrets", "year": 1998},
    {"id": 6, "title": "Harry Potter and the Philosopher's Stone", "year": 1997},
    {"id": 3, "title": "The Caves of Steel", "year": 1954}
  ],
  "number": 1,
  "size": 3,
  "numberOfElements": 3,
  "totalElements": 8,
  "totalPages": 3
}
```

Step 3. Request GET /api/books?sort=nosuchfield. The Whitelabel Error Page reports Internal Server Error and status=500. The failure occurs while constructing a sort using a nonexistent property. This is the observed negative-test result, not a successful response.

[Screenshot included in the separate Word/PDF report.]

Figure 17. The invalid sort property nosuchfield produces HTTP 500.

### 4.3. Compare Page and Slice

In a separate verification run, the endpoint was temporarily changed to Slice. SQL statements were counted for the same second-page request. Page was then restored, the project was rebuilt and tested, and the presence of totalElements was checked again. The local screenshots show the final Page version; the temporary Slice measurement comes from the verification run.

| Endpoint version | SQL for the measured request | Total-count fields |
| --- | --- | --- |
| Page, page=1&size=3&sort=year,desc | 2: data and count | totalElements=8, totalPages=3 |
| Slice, the same request | 1: data without count | Not present |

Page is retained for publication because the assignment requires the total number of records. Slice is cheaper when only the existence of another portion is needed. Two statements refer to the measured full middle page; for some incomplete pages Spring Data can avoid a count query.

Actual Hibernate output for both Page statements from the verification run:

```sql
Hibernate: 
    select
        b1_0.id,
        b1_0.archived,
        a1_0.id,
        a1_0.country,
        a1_0.name,
        b1_0.created_at,
        b1_0.genre,
        b1_0.title,
        b1_0.version,
        b1_0.year 
    from
        book b1_0 
    left join
        author a1_0 
            on a1_0.id=b1_0.author_id 
    order by
        b1_0.year desc 
    offset
        ? rows 
    fetch
        first ? rows only
Hibernate: 
    select
        count(b1_0.id) 
    from
        book b1_0
```

### 4.4. Commit and push

The IntelliJ screenshots show the local lab08 branch. The assignment specifies these completion commands:

```shell
git add .
git commit -m "lab08: PostgreSQL, entities, associations, queries, paging"
git push -u origin lab08
```

The lab08 branch is prepared locally. Remote publication must be verified after a successful git push.

## Individual assignment. Variant 9: a named entity graph

Book declares the Book.author graph, visible in Figure 4. Two different repository methods use it: findByGenre and findByTitleContainingIgnoreCase.

```java
@NamedEntityGraph(name = "Book.author",
    attributeNodes = @NamedAttributeNode("author"))

@EntityGraph(value = "Book.author")
List<Book> findByGenre(Genre genre);

@EntityGraph(value = "Book.author")
List<Book> findByTitleContainingIgnoreCase(String fragment);
```

| Method | Verification result | Author initialized | SQL statements |
| --- | --- | --- | --- |
| findByGenre(SCIENCE) | 3 books | Yes; checked with isLoaded | 1 |
| findByTitleContainingIgnoreCase("Harry") | 3 books | Yes; checked with isLoaded | 1 |

[Screenshot included in the separate Word/PDF report.]

Figure 18. Variant 9: two repository methods use the named graph Book.author; the repository screenshot is repeated here for the defence.

The graph fetches an author with each book, removing extra queries when its name is read. The tags collection is not included in the page graph, avoiding in-memory pagination caused by a collection fetch join.

## Final verification

The local screenshots confirm database creation, configuration, entities and associations, eight books, eleven book–tag links, foreign keys, Page responses and the invalid-sort error. The recorded verification run of the final version produced:

```text
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

This is the verification-run result. Successful push, N+1 measurements and the temporary Slice HTTP run are not shown in the supplied local screenshots; these actions are not attributed to screenshots of source code.

## Conclusions

Spring Data JPA replaced in-memory storage with PostgreSQL and provided CRUD through a repository without manually implementing the basic SQL operations. Correct persistence of associations requires an explicit foreign-key owner and consistent updates to both sides in Java. Verification reduced the query count from four to one using a fetch join, while the local screenshots demonstrated actual PostgreSQL rows and relationships. Page provides the total record count at the cost of a count query, whereas Slice supports navigation without a total count.

## Appendix A. Book listing

```java
package kz.iitu.springlab.catalog;

import jakarta.persistence.*;
import kz.iitu.springlab.web.Genre;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "book")
@NamedEntityGraph(name = "Book.author", attributeNodes = @NamedAttributeNode("author"))
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String title;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Genre genre;
    private int year;
    private boolean archived;
    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();
    @Version
    private long version;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;
    @ManyToMany
    @JoinTable(name = "book_tag", joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    protected Book() { }

    public Book(String title, Genre genre, int year) {
        this.title = title;
        this.genre = genre;
        this.year = year;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public Genre getGenre() { return genre; }
    public int getYear() { return year; }
    public boolean isArchived() { return archived; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
    public Author getAuthor() { return author; }
    public Set<Tag> getTags() { return tags; }
    public void setTitle(String title) { this.title = title; }
    public void setGenre(Genre genre) { this.genre = genre; }
    public void setYear(int year) { this.year = year; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public void setAuthor(Author author) { this.author = author; }

    public void addTag(Tag tag) { tags.add(tag); }
}
```

## Appendix B. BookRepository listing

```java
package kz.iitu.springlab.catalog;

import kz.iitu.springlab.web.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    @EntityGraph(value = "Book.author")
    List<Book> findByGenre(Genre genre);

    @EntityGraph(value = "Book.author")
    List<Book> findByTitleContainingIgnoreCase(String fragment);

    List<Book> findByYearBetweenOrderByYearDesc(int from, int to);
    List<Book> findTop3ByGenreOrderByYearDesc(Genre genre);
    long countByAuthorName(String name);

    @Query("""
            select b from Book b
            where b.genre = :genre and b.year >= :year
            order by b.title
            """)
    List<Book> search(@Param("genre") Genre genre, @Param("year") int year);

    @Modifying(clearAutomatically = true)
    @Query("update Book b set b.archived = true where b.year < :year")
    int archiveOlderThan(@Param("year") int year);

    @Query("select b from Book b join fetch b.author")
    List<Book> findAllWithAuthor();

    @Override
    @EntityGraph(value = "Book.author")
    Page<Book> findAll(Pageable pageable);

    @EntityGraph(value = "Book.author")
    Page<Book> findByGenre(Genre genre, Pageable pageable);

    @EntityGraph(value = "Book.author")
    @Query("select b from Book b where lower(b.author.name) = lower(:author)")
    Page<Book> findPageByAuthor(@Param("author") String author, Pageable pageable);

    @EntityGraph(value = "Book.author")
    @Query("select b from Book b where b.genre = :genre and lower(b.author.name) = lower(:author)")
    Page<Book> findPageByGenreAndAuthor(@Param("genre") Genre genre, @Param("author") String author, Pageable pageable);

    @EntityGraph(value = "Book.author")
    Slice<Book> findAllBy(Pageable pageable);
}
```

## Appendix C. Schema reported by the verification database

The following saved psql output is from PostgreSQL 16 for the same model version. It supplements the local PostgreSQL 18 screenshots and is not presented as output from the local computer.

```text
           List of relations
 Schema |   Name   | Type  |   Owner   
--------+----------+-------+-----------
 public | author   | table | springlab
 public | book     | table | springlab
 public | book_tag | table | springlab
 public | tag      | table | springlab
(4 rows)

                                        Table "public.book"
   Column   |            Type             | Collation | Nullable |             Default              
------------+-----------------------------+-----------+----------+----------------------------------
 archived   | boolean                     |           | not null | 
 year       | integer                     |           | not null | 
 author_id  | bigint                      |           |          | 
 created_at | timestamp(6) with time zone |           |          | 
 id         | bigint                      |           | not null | generated by default as identity
 version    | bigint                      |           | not null | 
 genre      | character varying(20)       |           | not null | 
 title      | character varying(120)      |           | not null | 
Indexes:
    "book_pkey" PRIMARY KEY, btree (id)
Check constraints:
    "book_genre_check" CHECK (genre::text = ANY (ARRAY['FICTION'::character varying, 'FANTASY'::character varying, 'SCIENCE'::character varying, 'HISTORY'::character varying, 'ROMANCE'::character varying]::text[]))
Foreign-key constraints:
    "fkklnrv3weler2ftkweewlky958" FOREIGN KEY (author_id) REFERENCES author(id)
Referenced by:
    TABLE "book_tag" CONSTRAINT "fkdrc33u5ufw8rdvajeveowgx7g" FOREIGN KEY (book_id) REFERENCES book(id)

              Table "public.book_tag"
 Column  |  Type  | Collation | Nullable | Default 
---------+--------+-----------+----------+---------
 book_id | bigint |           | not null | 
 tag_id  | bigint |           | not null | 
Indexes:
    "book_tag_pkey" PRIMARY KEY, btree (book_id, tag_id)
Foreign-key constraints:
    "fkdrc33u5ufw8rdvajeveowgx7g" FOREIGN KEY (book_id) REFERENCES book(id)
    "fkrxw4xl05l6ns1763bq284e7m2" FOREIGN KEY (tag_id) REFERENCES tag(id)

                                   Table "public.author"
 Column  |          Type          | Collation | Nullable |             Default              
---------+------------------------+-----------+----------+----------------------------------
 id      | bigint                 |           | not null | generated by default as identity
 country | character varying(255) |           |          | 
 name    | character varying(255) |           | not null | 
Indexes:
    "author_pkey" PRIMARY KEY, btree (id)
Referenced by:
    TABLE "book" CONSTRAINT "fkklnrv3weler2ftkweewlky958" FOREIGN KEY (author_id) REFERENCES author(id)
```
