package edu.mirea.vitality.blog.service;

import edu.mirea.vitality.blog.domain.dto.post.PostFilter;
import edu.mirea.vitality.blog.domain.dto.post.PostRequest;
import edu.mirea.vitality.blog.domain.dto.post.UpdatePostRequest;
import edu.mirea.vitality.blog.domain.model.Category;
import edu.mirea.vitality.blog.domain.model.Post;
import edu.mirea.vitality.blog.domain.model.User;
import edu.mirea.vitality.blog.exception.post.PostNotFoundProblem;
import edu.mirea.vitality.blog.exception.user.ForbiddenAccessProblem;
import edu.mirea.vitality.blog.repository.PostFileRelRepository;
import edu.mirea.vitality.blog.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository repository;
    private final FileService fileService;
    private final UserService userService;
    private final CategoryService categoryService;


    private final PostUserRelService postUserRelService;
    private final PostFileRelRepository postFileRelRepository;

    /**
     * Сохранение поста
     */
    public Post save(Post post) {
        return repository.save(post);
    }

    /**
     * Создание поста с прикрепленными файлами
     *
     * @param request Запрос на создание поста
     * @return Пост
     */
    @Transactional
    public Post create(PostRequest request) {


        var post = Post.builder()
                .title(request.title())
                .author(userService.getCurrentUser())
                .content(request.content())
                .build();
        return setPostInformation(request, post);
    }

    /**
     * Поиск поста по идентификатору
     *
     * @param id Идентификатор поста
     * @return Пост
     */
    public Optional<Post> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * Получение поста по идентификатору
     *
     * @param postId Идентификатор поста
     * @return Пост
     */
    public Post getById(Long postId) {
        return findById(postId).orElseThrow(() -> new PostNotFoundProblem(postId));
    }

    /**
     * Удаление поста по id
     *
     * @param postId id поста
     */
    @Transactional
    public void deleteById(Long postId) {
        User currentUser = userService.getCurrentUser();
        Post post = getById(postId);

        if (!post.isAuthor(currentUser) && !currentUser.isAdmin()) {
            throw new ForbiddenAccessProblem();
        }

        post.getAttachments().forEach(rel -> {
            fileService.deleteById(rel.getFile().getId());
            postFileRelRepository.delete(rel);
        });

        repository.deleteById(postId);
    }


    /**
     * Обновление поста
     *
     * @param request запрос на обновление поста
     * @param postId  id поста
     * @return обновленный пост
     */
    @Transactional
    public Post update(UpdatePostRequest request, Long postId) {
        var post = getById(postId);
        var currentUser = userService.getCurrentUser();
//        var topic = isNull(request.topicId()) ? null : topicService.getById(request.topicId());
//        var type = isNull(request.postTypeId()) ? null : postTypeService.getById(request.postTypeId());


        if (!post.isAuthor(currentUser)) {
            throw new ForbiddenAccessProblem();
        }

        // Проверяем что файлы, помеченные на удаление, принадлежат посту
        if (!isNull(request.deletedAttachments())) {
            request.deletedAttachments().forEach(fileId -> {
                if (post.getAttachments().stream().noneMatch(rel -> rel.getFile().getId().equals(fileId))) {
                    throw new ForbiddenAccessProblem();
                }
            });
        }

        // Удаляем файлы, помеченные на удаление
        if (!isNull(request.deletedAttachments())) {
            post.getAttachments().stream()
                    // Получаем только те связи, которые помечены на удаление
                    .filter(rel -> request.deletedAttachments().contains(rel.getFile().getId()))
                    .forEach(rel -> {
                        post.getAttachments().remove(rel);
                        fileService.deleteById(rel.getFile().getId());
                    });
        }

        // Загружаем новые файлы
        if (!isNull(request.attachments())) {
            post.addFiles(fileService.uploadFiles(request.attachments()));
        }

        post.setTitle(request.title());
        post.setContent(request.content());

        return save(post);
    }


    /**
     * Заполнение полей поста
     *
     * @param request запрос
     * @param post    пост, который нужно заполнить
     * @return сохраненный пост
     */
    private Post setPostInformation(PostRequest request, Post post) {
        if (!isNull(request.categoryId())) {
            Category category = categoryService.getById(request.categoryId());
            post.setCategory(category);
        }

        if (!isNull(request.attachments())) {
            post.addFiles(fileService.uploadFiles(request.attachments()));
        }
        return save(post);
    }

    /**
     * Получение по фильтру
     *
     * @param filter фильтр
     * @return Найденные посты
     */
    public Page<Post> findByFilter(PostFilter filter) {

        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize());

        return repository.findAllWithPages(filter.getCategoryId(), filter.getTitle(), pageable);
    }


    /**
     * Удаление постов по id автора
     *
     * @param id id автора
     */
    @Transactional
    public void deleteByAuthorId(Long id) {
        var posts = repository.findAllByAuthorId(id);
        posts.forEach(post -> deleteById(post.getId()));
    }



    public List<Post> getAllByAuthorId(Long id) {
        return repository.findAllByAuthorId(id);
    }


    @Transactional
    public void likePost(Long postId) {
        Post post = getById(postId);

        User user = userService.getCurrentUser();
        if (!postUserRelService.existsByPostIdAndUserId(postId, user.getId())) {
            postUserRelService.create(post, user);
            post.setLikes(post.getLikes() + 1);
            repository.save(post);
        }
    }


    @Transactional
    public void unlikePost(Long postId) {
        Post post = getById(postId);

        User user = userService.getCurrentUser();
        if (postUserRelService.existsByPostIdAndUserId(postId, user.getId())) {
            postUserRelService.delete(postId, user.getId());
            post.setLikes(post.getLikes() - 1);
            repository.save(post);
        }
    }

}