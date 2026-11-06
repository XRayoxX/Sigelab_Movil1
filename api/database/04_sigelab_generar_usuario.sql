/* ==========================================================================
   SIGELAB - Script 04: Generación automática del nombre de usuario
   Ejecutar DESPUÉS de 01 (y antes de crear usuarios nuevos desde la aplicación).
   --------------------------------------------------------------------------
   Cómo funciona:
   1) Java inserta el usuario SIN NombreUsuario.
   2) El DEFAULT DF_Usuario_NombreUsuario_Auto pone un valor temporal 'AUTO_...'.
   3) El trigger TR_Usuario_GenerarNombreUsuario detecta 'AUTO_' y llama a
      dbo.GenerarNombreUsuario, que prueba (sin tildes ni ñ) el primer nombre libre de:
      inicial + 1er apellido, luego + inicial del 2do nombre, luego + inicial del
      2do apellido, y por último agrega un número.
      Ej.: Juan Carlos Pérez López -> jperez, jcperez, jcperezl, jcperezl2...
   4) Java lee el nombre generado y se lo muestra al administrador.
   dbo.RegistrarUsuario hace lo mismo como procedimiento (alternativa desde SQL).
   Definiciones copiadas tal cual de la base de Azure (sigelabadministrador).
   ========================================================================== */

USE Sigelab;
GO

IF NOT EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = 'DF_Usuario_NombreUsuario_Auto')
    ALTER TABLE dbo.Usuario
        ADD CONSTRAINT DF_Usuario_NombreUsuario_Auto
        DEFAULT ('AUTO_' + LEFT(REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''), 25)) FOR NombreUsuario;
GO

CREATE OR ALTER PROCEDURE dbo.GenerarNombreUsuario
    @PrimerNombre     NVARCHAR(100),
    @SegundoNombre    NVARCHAR(100) = NULL,
    @PrimerApellido   NVARCHAR(100),
    @SegundoApellido  NVARCHAR(100) = NULL,
    @NombreGenerado   VARCHAR(30) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE
        @N1 NVARCHAR(100),
        @N2 NVARCHAR(100),
        @A1 NVARCHAR(100),
        @A2 NVARCHAR(100),
        @Base VARCHAR(200),
        @Candidato VARCHAR(30),
        @Numero INT = 2,
        @Indice INT = 1,
        @Total INT,
        @Sufijo VARCHAR(12);

    -- Normalizar nombres
    SET @N1 = LOWER(TRIM(ISNULL(@PrimerNombre, '')));
    SET @N2 = LOWER(TRIM(ISNULL(@SegundoNombre, '')));
    SET @A1 = LOWER(TRIM(ISNULL(@PrimerApellido, '')));
    SET @A2 = LOWER(TRIM(ISNULL(@SegundoApellido, '')));

    -- Quitar tildes
    SET @N1 = TRANSLATE(@N1, N'áéíóúü', N'aeiouu');
    SET @N2 = TRANSLATE(@N2, N'áéíóúü', N'aeiouu');
    SET @A1 = TRANSLATE(@A1, N'áéíóúü', N'aeiouu');
    SET @A2 = TRANSLATE(@A2, N'áéíóúü', N'aeiouu');

    -- Sustituir ñ y eliminar separadores comunes
    SET @N1 = REPLACE(REPLACE(REPLACE(@N1, N'ñ', N'n'), N' ', N''), N'-', N'');
    SET @N2 = REPLACE(REPLACE(REPLACE(@N2, N'ñ', N'n'), N' ', N''), N'-', N'');
    SET @A1 = REPLACE(REPLACE(REPLACE(@A1, N'ñ', N'n'), N' ', N''), N'-', N'');
    SET @A2 = REPLACE(REPLACE(REPLACE(@A2, N'ñ', N'n'), N' ', N''), N'-', N'');

    IF @N1 = '' OR @A1 = ''
    BEGIN
        THROW 50002, 'Primer nombre y primer apellido son obligatorios.', 1;
    END;

    IF @N1 LIKE N'%[^a-z]%' COLLATE Latin1_General_100_BIN2
       OR @N2 LIKE N'%[^a-z]%' COLLATE Latin1_General_100_BIN2
       OR @A1 LIKE N'%[^a-z]%' COLLATE Latin1_General_100_BIN2
       OR @A2 LIKE N'%[^a-z]%' COLLATE Latin1_General_100_BIN2
    BEGIN
        THROW 50003, 'Los nombres contienen caracteres no admitidos para generar el usuario.', 1;
    END;

    DECLARE @Bases TABLE (
        Id INT IDENTITY(1,1),
        Valor VARCHAR(200) UNIQUE
    );

    -- Primera inicial + primer apellido
    INSERT INTO @Bases (Valor)
    VALUES (
        CONVERT(VARCHAR(200), LEFT(@N1, 1) + @A1)
    );

    -- Agregar inicial del segundo nombre
    IF @N2 <> ''
    BEGIN
        INSERT INTO @Bases (Valor)
        VALUES (
            CONVERT(VARCHAR(200),
                LEFT(@N1, 1) + LEFT(@N2, 1) + @A1
            )
        );
    END;

    -- Agregar inicial del segundo apellido
    IF @A2 <> ''
    BEGIN
        SET @Base = CONVERT(VARCHAR(200),
            LEFT(@N1, 1)
            + CASE WHEN @N2 <> ''
                   THEN LEFT(@N2, 1)
                   ELSE ''
              END
            + @A1
            + LEFT(@A2, 1)
        );

        IF NOT EXISTS (
            SELECT 1 FROM @Bases WHERE Valor = @Base
        )
        BEGIN
            INSERT INTO @Bases (Valor)
            VALUES (@Base);
        END;
    END;

    SELECT @Total = COUNT(*) FROM @Bases;

    -- Probar candidatos sin números
    WHILE @Indice <= @Total
    BEGIN
        SELECT @Base = Valor
        FROM @Bases
        WHERE Id = @Indice;

        SET @Candidato = LEFT(@Base, 30);

        IF NOT EXISTS (
            SELECT 1
            FROM dbo.Usuario
            WHERE NombreUsuario COLLATE Latin1_General_100_CI_AI
                = @Candidato COLLATE Latin1_General_100_CI_AI
        )
        BEGIN
            SET @NombreGenerado = @Candidato;
            RETURN;
        END;

        SET @Indice = @Indice + 1;
    END;

    -- Agregar números consecutivos al último candidato
    SELECT TOP (1) @Base = Valor
    FROM @Bases
    ORDER BY Id DESC;

    WHILE @Numero <= 999999
    BEGIN
        SET @Sufijo = CONVERT(VARCHAR(12), @Numero);

        SET @Candidato =
            LEFT(@Base, 30 - LEN(@Sufijo)) + @Sufijo;

        IF NOT EXISTS (
            SELECT 1
            FROM dbo.Usuario
            WHERE NombreUsuario COLLATE Latin1_General_100_CI_AI
                = @Candidato COLLATE Latin1_General_100_CI_AI
        )
        BEGIN
            SET @NombreGenerado = @Candidato;
            RETURN;
        END;

        SET @Numero = @Numero + 1;
    END;

    THROW 50004, 'No fue posible generar un nombre de usuario.', 1;
END;
GO

CREATE OR ALTER PROCEDURE dbo.RegistrarUsuario
    @PrimerNombre NVARCHAR(100),
    @SegundoNombre NVARCHAR(100) = NULL,
    @PrimerApellido NVARCHAR(100),
    @SegundoApellido NVARCHAR(100) = NULL,
    @ContrasenaHash VARCHAR(64),
    @Correo VARCHAR(100),
    @IdRol INT,
    @Estado VARCHAR(10) = 'ACTIVO',
    @IdUsuarioGenerado INT OUTPUT,
    @NombreUsuarioGenerado VARCHAR(30) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @ResultadoBloqueo INT;

    SET @IdUsuarioGenerado = NULL;
    SET @NombreUsuarioGenerado = NULL;

    -- Validaciones previas
    IF NULLIF(TRIM(@PrimerNombre), '') IS NULL
       OR NULLIF(TRIM(@PrimerApellido), '') IS NULL
    BEGIN
        THROW 50010, 'El primer nombre y apellido son obligatorios.', 1;
    END;

    IF LEN(TRIM(@PrimerNombre)) > 60
       OR LEN(TRIM(@PrimerApellido)) > 60
    BEGIN
        THROW 50011, 'Nombre o apellido excede los 60 caracteres.', 1;
    END;

    IF @ContrasenaHash IS NULL
       OR LEN(@ContrasenaHash) <> 64
    BEGIN
        THROW 50012, 'El hash debe tener exactamente 64 caracteres.', 1;
    END;

    IF @Correo IS NULL OR @IdRol IS NULL
    BEGIN
        THROW 50013, 'Correo y rol son obligatorios.', 1;
    END;

    IF @Estado NOT IN ('ACTIVO', 'INACTIVO')
       OR @Estado IS NULL
    BEGIN
        THROW 50014, 'Estado no permitido.', 1;
    END;

    BEGIN TRY
        BEGIN TRANSACTION;

        -- Serializar registros realizados por este procedimiento
        EXEC @ResultadoBloqueo = sys.sp_getapplock
            @Resource = 'SIGELAB_REGISTRO_USUARIO',
            @LockMode = 'Exclusive',
            @LockOwner = 'Transaction',
            @LockTimeout = 10000;

        IF @ResultadoBloqueo < 0
        BEGIN
            THROW 50015, 'No se pudo obtener el bloqueo de registro.', 1;
        END;

        -- Generar el nombre de usuario disponible
        EXEC dbo.GenerarNombreUsuario
            @PrimerNombre = @PrimerNombre,
            @SegundoNombre = @SegundoNombre,
            @PrimerApellido = @PrimerApellido,
            @SegundoApellido = @SegundoApellido,
            @NombreGenerado = @NombreUsuarioGenerado OUTPUT;

        -- Registrar usuario
        INSERT INTO dbo.Usuario
        (
            Nombre,
            SegundoNombre,
            Apellido,
            SegundoApellido,
            NombreUsuario,
            ContrasenaHash,
            Correo,
            IdRol,
            Estado
        )
        VALUES
        (
            TRIM(@PrimerNombre),
            NULLIF(TRIM(@SegundoNombre), ''),
            TRIM(@PrimerApellido),
            NULLIF(TRIM(@SegundoApellido), ''),
            @NombreUsuarioGenerado,
            @ContrasenaHash,
            @Correo,
            @IdRol,
            @Estado
        );

        SET @IdUsuarioGenerado = CONVERT(INT, SCOPE_IDENTITY());

        COMMIT TRANSACTION;

        SELECT
            @IdUsuarioGenerado AS IdUsuario,
            @NombreUsuarioGenerado AS NombreUsuario,
            'Usuario registrado correctamente' AS Mensaje;

    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0
            ROLLBACK TRANSACTION;

        THROW;
    END CATCH;
END;
GO

CREATE OR ALTER TRIGGER dbo.TR_Usuario_GenerarNombreUsuario
ON dbo.Usuario
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    IF NOT EXISTS (
        SELECT 1
        FROM inserted
        WHERE NombreUsuario LIKE 'AUTO[_]%'
    )
        RETURN;

    DECLARE
        @IdUsuario INT,
        @PrimerNombre NVARCHAR(100),
        @SegundoNombre NVARCHAR(100),
        @PrimerApellido NVARCHAR(100),
        @SegundoApellido NVARCHAR(100),
        @NombreGenerado VARCHAR(30),
        @ResultadoBloqueo INT;

    EXEC @ResultadoBloqueo = sys.sp_getapplock
        @Resource = 'SIGELAB_REGISTRO_USUARIO',
        @LockMode = 'Exclusive',
        @LockOwner = 'Transaction',
        @LockTimeout = 10000;

    IF @ResultadoBloqueo < 0
    BEGIN
        THROW 50020,
              'No se pudo obtener el bloqueo para generar usuarios.',
              1;
    END;

    DECLARE cursorUsuarios CURSOR LOCAL FAST_FORWARD FOR
        SELECT
            IdUsuario,
            Nombre,
            SegundoNombre,
            Apellido,
            SegundoApellido
        FROM inserted
        WHERE NombreUsuario LIKE 'AUTO[_]%'
        ORDER BY IdUsuario;

    OPEN cursorUsuarios;

    FETCH NEXT FROM cursorUsuarios INTO
        @IdUsuario,
        @PrimerNombre,
        @SegundoNombre,
        @PrimerApellido,
        @SegundoApellido;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @NombreGenerado = NULL;

        EXEC dbo.GenerarNombreUsuario
            @PrimerNombre = @PrimerNombre,
            @SegundoNombre = @SegundoNombre,
            @PrimerApellido = @PrimerApellido,
            @SegundoApellido = @SegundoApellido,
            @NombreGenerado = @NombreGenerado OUTPUT;

        IF NULLIF(@NombreGenerado, '') IS NULL
        BEGIN
            CLOSE cursorUsuarios;
            DEALLOCATE cursorUsuarios;

            THROW 50021,
                  'No fue posible generar un nombre de usuario.',
                  1;
        END;

        UPDATE dbo.Usuario
        SET NombreUsuario = @NombreGenerado
        WHERE IdUsuario = @IdUsuario;

        FETCH NEXT FROM cursorUsuarios INTO
            @IdUsuario,
            @PrimerNombre,
            @SegundoNombre,
            @PrimerApellido,
            @SegundoApellido;
    END;

    CLOSE cursorUsuarios;
    DEALLOCATE cursorUsuarios;
END;
GO

PRINT 'Generación automática de nombre de usuario instalada.';
GO
