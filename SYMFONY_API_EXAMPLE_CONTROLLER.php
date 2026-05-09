/**
 * EXAMPLE SYMFONY CONTROLLER
 * 
 * This file shows how to implement the authentication endpoints on your Symfony backend.
 * Copy this to: src/Controller/ApiAuthController.php
 * 
 * ⚠️ NOTE: This is a REFERENCE implementation. Adapt to your actual Symfony setup!
 */

namespace App\Controller;

use App\Entity\User;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;
use Lexik\Bundle\JWTAuthenticationBundle\Services\JWTTokenManagerInterface;

#[Route('/api')]
class ApiAuthController extends AbstractController
{
    private UserPasswordHasherInterface $passwordHasher;
    private EntityManagerInterface $entityManager;
    private JWTTokenManagerInterface $jwtManager;

    public function __construct(
        UserPasswordHasherInterface $passwordHasher,
        EntityManagerInterface $entityManager,
        JWTTokenManagerInterface $jwtManager
    ) {
        $this->passwordHasher = $passwordHasher;
        $this->entityManager = $entityManager;
        $this->jwtManager = $jwtManager;
    }

    /**
     * POST /api/login
     * 
     * Authentifie un utilisateur et retourne un JWT token.
     * 
     * Request:
     * {
     *   "email": "user@example.com",
     *   "password": "plaintext_password"
     * }
     * 
     * Response (200):
     * {
     *   "id": 1,
     *   "email": "user@example.com",
     *   "nom": "Dupont",
     *   "prenom": "Jean",
     *   "type": "etudiant",
     *   "roles": ["ROLE_ETUDIANT"],
     *   "status": "actif",
     *   "photo": "https://cloudinary.url/photo.jpg",
     *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     * }
     * 
     * Response (401):
     * {
     *   "error": "Invalid credentials"
     * }
     */
    #[Route('/login', methods: ['POST'])]
    public function login(Request $request): JsonResponse
    {
        $data = json_decode($request->getContent(), true);

        // ✅ Valider les données entrantes
        if (!isset($data['email']) || !isset($data['password'])) {
            return $this->json(['error' => 'Missing email or password'], 400);
        }

        $email = $data['email'];
        $plainPassword = $data['password'];

        // ✅ Chercher l'utilisateur par email
        $user = $this->entityManager->getRepository(User::class)->findOneBy(['email' => $email]);

        if (!$user) {
            // ❌ Réponse générique pour éviter l'énumération d'utilisateurs
            return $this->json(['error' => 'Invalid credentials'], 401);
        }

        // ✅ Vérifier le mot de passe (Symfony le compare avec le hash)
        if (!$this->passwordHasher->isPasswordValid($user, $plainPassword)) {
            return $this->json(['error' => 'Invalid credentials'], 401);
        }

        // ✅ Vérifier que l'utilisateur n'est pas banni
        if ($user->getStatus() === 'ban') {
            return $this->json(['error' => 'Account banned'], 403);
        }

        // ✅ Générer le JWT token
        $token = $this->jwtManager->create($user);

        // ✅ Retourner l'utilisateur avec le token
        return $this->json([
            'id' => $user->getId(),
            'email' => $user->getEmail(),
            'nom' => $user->getNom(),
            'prenom' => $user->getPrenom(),
            'type' => $user->getType(),
            'roles' => $user->getRoles(),
            'status' => $user->getStatus(),
            'photo' => $user->getPhoto(),
            'token' => $token
        ], 200);
    }

    /**
     * POST /api/register
     * 
     * Enregistre un nouvel utilisateur.
     * 
     * Request:
     * {
     *   "email": "newuser@example.com",
     *   "password": "plaintext_password",
     *   "nom": "Smith",
     *   "prenom": "Alice",
     *   "telephone": "+216 12 345 678",
     *   "type": "etudiant"
     * }
     * 
     * Response (201):
     * {
     *   "id": 2,
     *   "email": "newuser@example.com",
     *   "nom": "Smith",
     *   "prenom": "Alice",
     *   "type": "etudiant",
     *   "roles": ["ROLE_ETUDIANT"],
     *   "status": "actif",
     *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     * }
     * 
     * Response (400):
     * {
     *   "error": "Email already exists"
     * }
     */
    #[Route('/register', methods: ['POST'])]
    public function register(Request $request): JsonResponse
    {
        $data = json_decode($request->getContent(), true);

        // ✅ Valider les données obligatoires
        $required = ['email', 'password', 'nom', 'prenom'];
        foreach ($required as $field) {
            if (!isset($data[$field]) || empty($data[$field])) {
                return $this->json(['error' => "Missing required field: $field"], 400);
            }
        }

        $email = trim($data['email']);
        $plainPassword = $data['password'];
        $nom = trim($data['nom']);
        $prenom = trim($data['prenom']);
        $telephone = $data['telephone'] ?? null;
        $type = $data['type'] ?? 'etudiant';

        // ✅ Vérifier que l'email n'existe pas déjà
        $existingUser = $this->entityManager->getRepository(User::class)->findOneBy(['email' => $email]);
        if ($existingUser) {
            return $this->json(['error' => 'Email already exists'], 409);
        }

        // ✅ Valider le format email
        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            return $this->json(['error' => 'Invalid email format'], 400);
        }

        // ✅ Valider la force du mot de passe (minimum 6 caractères)
        if (strlen($plainPassword) < 6) {
            return $this->json(['error' => 'Password must be at least 6 characters'], 400);
        }

        // ✅ Créer un nouvel utilisateur
        $user = new User();
        $user->setEmail($email);
        $user->setNom($nom);
        $user->setPrenom($prenom);
        if ($telephone) {
            $user->setTelephone($telephone);
        }
        
        // Normaliser le type (etudiant, prof, admin)
        $validTypes = ['etudiant', 'prof', 'admin'];
        $type = strtolower($type);
        if (!in_array($type, $validTypes)) {
            $type = 'etudiant';
        }
        $user->setType($type);
        
        // Assigner les rôles en fonction du type
        $roles = match ($type) {
            'prof' => ['ROLE_PROF'],
            'admin' => ['ROLE_ADMIN'],
            default => ['ROLE_ETUDIANT'],
        };
        $user->setRoles($roles);
        
        $user->setStatus('actif');

        // ✅ IMPORTANT: Hacher le mot de passe avec Symfony
        $hashedPassword = $this->passwordHasher->hashPassword($user, $plainPassword);
        $user->setPassword($hashedPassword);

        // ✅ Persister l'utilisateur
        $this->entityManager->persist($user);
        $this->entityManager->flush();

        // ✅ Générer le JWT token
        $token = $this->jwtManager->create($user);

        // ✅ Retourner l'utilisateur créé avec le token
        return $this->json([
            'id' => $user->getId(),
            'email' => $user->getEmail(),
            'nom' => $user->getNom(),
            'prenom' => $user->getPrenom(),
            'type' => $user->getType(),
            'roles' => $user->getRoles(),
            'status' => $user->getStatus(),
            'token' => $token
        ], 201);
    }

    /**
     * GET /api/user/profile
     * 
     * Récupère le profil de l'utilisateur connecté (protégé par JWT).
     * Nécessite le header: Authorization: Bearer <token>
     * 
     * Response (200):
     * {
     *   "id": 1,
     *   "email": "user@example.com",
     *   "nom": "Dupont",
     *   "prenom": "Jean",
     *   "type": "etudiant",
     *   "roles": ["ROLE_ETUDIANT"],
     *   "status": "actif"
     * }
     */
    #[Route('/user/profile', methods: ['GET'])]
    #[IsGranted('ROLE_USER')]
    public function getProfile(): JsonResponse
    {
        $user = $this->getUser();

        if (!$user) {
            return $this->json(['error' => 'Unauthorized'], 401);
        }

        return $this->json([
            'id' => $user->getId(),
            'email' => $user->getEmail(),
            'nom' => $user->getNom(),
            'prenom' => $user->getPrenom(),
            'type' => $user->getType(),
            'roles' => $user->getRoles(),
            'status' => $user->getStatus(),
            'photo' => $user->getPhoto()
        ], 200);
    }

    /**
     * PUT /api/user/profile
     * 
     * Met à jour le profil de l'utilisateur connecté (protégé par JWT).
     */
    #[Route('/user/profile', methods: ['PUT'])]
    #[IsGranted('ROLE_USER')]
    public function updateProfile(Request $request): JsonResponse
    {
        $user = $this->getUser();

        if (!$user) {
            return $this->json(['error' => 'Unauthorized'], 401);
        }

        $data = json_decode($request->getContent(), true);

        // ✅ Mettre à jour les champs autorisés
        if (isset($data['nom'])) {
            $user->setNom($data['nom']);
        }
        if (isset($data['prenom'])) {
            $user->setPrenom($data['prenom']);
        }
        if (isset($data['telephone'])) {
            $user->setTelephone($data['telephone']);
        }

        $this->entityManager->flush();

        return $this->json([
            'message' => 'Profile updated successfully',
            'id' => $user->getId(),
            'email' => $user->getEmail(),
            'nom' => $user->getNom(),
            'prenom' => $user->getPrenom()
        ], 200);
    }

    /**
     * POST /api/user/change-password
     * 
     * Change le mot de passe de l'utilisateur connecté (protégé par JWT).
     * 
     * Request:
     * {
     *   "current_password": "old_password",
     *   "new_password": "new_password"
     * }
     */
    #[Route('/user/change-password', methods: ['POST'])]
    #[IsGranted('ROLE_USER')]
    public function changePassword(Request $request): JsonResponse
    {
        $user = $this->getUser();

        if (!$user) {
            return $this->json(['error' => 'Unauthorized'], 401);
        }

        $data = json_decode($request->getContent(), true);

        // ✅ Valider les données
        if (!isset($data['current_password']) || !isset($data['new_password'])) {
            return $this->json(['error' => 'Missing password fields'], 400);
        }

        // ✅ Vérifier le mot de passe actuel
        if (!$this->passwordHasher->isPasswordValid($user, $data['current_password'])) {
            return $this->json(['error' => 'Current password is incorrect'], 401);
        }

        // ✅ Valider le nouveau mot de passe
        if (strlen($data['new_password']) < 6) {
            return $this->json(['error' => 'New password must be at least 6 characters'], 400);
        }

        // ✅ Hacher et mettre à jour
        $hashedPassword = $this->passwordHasher->hashPassword($user, $data['new_password']);
        $user->setPassword($hashedPassword);
        $this->entityManager->flush();

        return $this->json(['message' => 'Password changed successfully'], 200);
    }
}

/**
 * IMPORTANT SETUP STEPS
 * 
 * 1. Install JWT Bundle:
 *    composer require lexik/jwt-authentication-bundle
 * 
 * 2. Generate JWT keys:
 *    php bin/console lexik:jwt:generate-keypair
 * 
 * 3. Configure config/packages/lexik_jwt_authentication.yaml:
 *    lexik_jwt_authentication:
 *      secret_key: '%kernel.project_dir%/config/jwt/private.pem'
 *      public_key: '%kernel.project_dir%/config/jwt/public.pem'
 *      pass_phrase: 'your_pass_phrase'
 * 
 * 4. Add routes to config/routes.yaml:
 *    api_auth:
 *      resource: App\Controller\ApiAuthController
 *      type: annotation
 * 
 * 5. Enable CORS in config/packages/nelmio_cors.yaml:
 *    nelmio_cors:
 *      defaults:
 *        allow_credentials: true
 *        allow_origin: ['https://javafx-client', 'http://localhost:*']
 *        allow_methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS']
 *        allow_headers: ['*']
 */
