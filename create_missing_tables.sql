CREATE TABLE IF NOT EXISTS `user` (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100),
    prenom VARCHAR(100),
    email VARCHAR(150) UNIQUE,
    password VARCHAR(255),
    CIN VARCHAR(20),
    telephone VARCHAR(20),
    adresse VARCHAR(255),
    date_naissance DATE,
    photo VARCHAR(500),
    photo_face VARCHAR(500),
    roles VARCHAR(255),
    type VARCHAR(50),
    status VARCHAR(20) DEFAULT 'actif',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS etudiant (
    id INT PRIMARY KEY,
    niveau VARCHAR(50),
    status VARCHAR(50),
    suspended_until DATETIME,
    filiere_id INT,
    FOREIGN KEY (id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS prof (
    id INT PRIMARY KEY,
    specialite VARCHAR(255),
    FOREIGN KEY (id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS matiere (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255),
    description TEXT,
    filiere_id INT,
    prof_id INT,
    is_visible TINYINT(1) DEFAULT 1,
    FOREIGN KEY (filiere_id) REFERENCES filiere(id) ON DELETE SET NULL,
    FOREIGN KEY (prof_id) REFERENCES prof(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notification (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    titre VARCHAR(255),
    message TEXT,
    lu TINYINT(1) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
