'use strict';

/**
 * Demo data seeder.
 *
 * Creates:
 * - One teacher account (email/password) with a classroom + topic.
 * - Five student accounts (email/password) enrolled in that classroom.
 * - Learning materials built from the docx files under `Seed doc materials`.
 *
 * Requirements:
 * - Service account credentials (GOOGLE_APPLICATION_CREDENTIALS) or emulator env.
 * - Run from repo root: `npm --prefix functions run seed:demo`
 * - Optional env overrides:
 *   SEED_PASSWORD, SEED_TEACHER_EMAIL, SEED_TEACHER_NAME,
 *   SEED_STUDENT_EMAILS (comma-separated),
 *   SEED_CLASSROOM_ID, SEED_CLASSROOM_NAME, SEED_JOIN_CODE,
 *   SEED_TOPIC_ID, SEED_TOPIC_NAME,
 *   SEED_DOC_DIR, SEED_MATERIAL_LIMIT,
 *   SEED_RESET_PASSWORDS=false (to keep existing passwords)
 */

const admin = require('firebase-admin');
const fs = require('fs/promises');
const { existsSync } = require('fs');
const path = require('path');

let mammoth = null;
try {
  // Optional: extracts docx text for richer material bodies.
  mammoth = require('mammoth');
} catch (error) {
  console.warn('[seed] mammoth not installed; materials will fall back to filenames only');
}

const DEFAULT_PASSWORD = process.env.SEED_PASSWORD || 'password';
const SHOULD_RESET_PASSWORDS = (process.env.SEED_RESET_PASSWORDS || 'true').toLowerCase() !== 'false';
const MATERIAL_LIMIT = parseInt(process.env.SEED_MATERIAL_LIMIT || '25', 10);
const SEED_DOC_DIR = process.env.SEED_DOC_DIR || path.resolve(__dirname, '..', '..', 'Seed doc materials');

const teacherConfig = {
  email: process.env.SEED_TEACHER_EMAIL || 'teacher1@gmail.com',
  displayName: process.env.SEED_TEACHER_NAME || 'Teacher One'
};

const defaultStudents = Array.from({ length: 5 }).map((_, index) => {
  const num = index + 1;
  return {
    email: `student${num}@gmail.com`,
    displayName: `Student ${num}`
  };
});

const studentConfig = (() => {
  const fromEnv = process.env.SEED_STUDENT_EMAILS;
  if (!fromEnv) return defaultStudents;
  return fromEnv
    .split(',')
    .map((email) => email.trim())
    .filter(Boolean)
    .map((email, idx) => ({
      email,
      displayName: `Student ${idx + 1}`
    }));
})();

const classroomConfig = {
  id: process.env.SEED_CLASSROOM_ID || 'demo-classroom',
  name: process.env.SEED_CLASSROOM_NAME || 'Demo Classroom',
  grade: '7',
  subject: 'Math',
  joinCode: process.env.SEED_JOIN_CODE || generateJoinCode()
};

const TOPIC_SEEDS = [
  {
    slug: 'illustrating-simple-and-compound-interest',
    name: 'Illustrating Simple and Compound Interest',
    description: 'Visual and numerical examples comparing simple vs. compound growth.'
  },
  {
    slug: 'distinguishing-simple-vs-compound-interest',
    name: 'Distinguishing Between Simple and Compound Interest',
    description: 'Identify when interest is simple vs. compounded and the impact over time.'
  },
  {
    slug: 'computing-values-in-interest-environments',
    name: 'Computing Interest, Maturity Value, Future Value and Present Value in Simple and Compound Interest Environment',
    description: 'Compute FV, PV, maturity values, and interest using both simple and compound models.'
  },
  {
    slug: 'solving-problems-involving-interest',
    name: 'Solving Problems Involving Simple and Compound Interest',
    description: 'Word problems that apply rates, periods, and compounding to real scenarios.'
  }
];

function generateJoinCode() {
  const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let result = '';
  for (let i = 0; i < 6; i += 1) {
    result += alphabet[Math.floor(Math.random() * alphabet.length)];
  }
  return result;
}

function slugify(value) {
  return value
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .replace(/-{2,}/g, '-')
    .slice(0, 64) || `material-${Date.now()}`;
}

function titleCase(value) {
  return value
    .split(' ')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

function normalizeWhitespace(value) {
  return value.replace(/\s+/g, ' ').trim();
}

function truncate(value, max) {
  if (!value) return '';
  return value.length <= max ? value : `${value.slice(0, max - 3)}...`;
}

function buildTopicConfigs() {
  return TOPIC_SEEDS.map((seed, index) => {
    const slug = slugify(seed.slug || seed.name);
    const overrideId = index === 0 ? process.env.SEED_TOPIC_ID : null;
    const overrideName = index === 0 ? process.env.SEED_TOPIC_NAME : null;
    return {
      id: overrideId || `${classroomConfig.id}-${slug}`,
      name: overrideName || seed.name,
      description: seed.description
    };
  });
}

async function ensurePasswordUser(auth, { email, displayName }, role) {
  const desired = { email, password: DEFAULT_PASSWORD, displayName };
  try {
    const existing = await auth.getUserByEmail(email);
    const updates = {};
    if (displayName && existing.displayName !== displayName) {
      updates.displayName = displayName;
    }
    if (SHOULD_RESET_PASSWORDS) {
      updates.password = DEFAULT_PASSWORD;
    }
    if (Object.keys(updates).length > 0) {
      await auth.updateUser(existing.uid, updates);
    }
    console.log(`[seed][${role}] Using existing auth user ${email}`);
    return {
      uid: existing.uid,
      email: existing.email || email,
      displayName: updates.displayName || existing.displayName || displayName,
      createdAt: existing.metadata?.creationTime
        ? new Date(existing.metadata.creationTime).getTime()
        : Date.now(),
      wasCreated: false
    };
  } catch (error) {
    if (error?.code !== 'auth/user-not-found') {
      throw error;
    }
    const created = await auth.createUser(desired);
    console.log(`[seed][${role}] Created auth user ${email}`);
    return {
      uid: created.uid,
      email: created.email || email,
      displayName: created.displayName || displayName,
      createdAt: created.metadata?.creationTime
        ? new Date(created.metadata.creationTime).getTime()
        : Date.now(),
      wasCreated: true
    };
  }
}

async function upsertTeacherProfile(db, teacher) {
  const ref = db.collection('teachers').doc(teacher.uid);
  const existing = await ref.get();
  const createdAt = existing.exists && typeof existing.get('createdAt') === 'number'
    ? existing.get('createdAt')
    : teacher.createdAt || Date.now();
  await ref.set(
    {
      displayName: teacher.displayName,
      email: teacher.email,
      createdAt,
      lastActive: Date.now()
    },
    { merge: true }
  );
}

async function upsertStudentProfile(db, student) {
  const ref = db.collection('students').doc(student.uid);
  const existing = await ref.get();
  const createdAt = existing.exists && typeof existing.get('createdAt') === 'number'
    ? existing.get('createdAt')
    : student.createdAt || Date.now();
  await ref.set(
    {
      displayName: student.displayName,
      email: student.email,
      createdAt
    },
    { merge: true }
  );
}

async function upsertClassroom(db, teacherId, studentIds) {
  const ref = db.collection('classrooms').doc(classroomConfig.id);
  const existing = await ref.get();
  const createdAt = existing.exists && typeof existing.get('createdAt') === 'number'
    ? existing.get('createdAt')
    : Date.now();
  const joinCode = existing.exists && existing.get('joinCode')
    ? existing.get('joinCode')
    : classroomConfig.joinCode;
  const uniqueStudents = Array.from(new Set(studentIds)).filter(Boolean);
  await ref.set(
    {
      teacherId,
      name: classroomConfig.name,
      grade: classroomConfig.grade,
      subject: classroomConfig.subject,
      joinCode,
      createdAt,
      updatedAt: Date.now(),
      isArchived: false,
      archived: false,
      archivedAt: null,
      students: uniqueStudents
    },
    { merge: true }
  );
}

async function extractDocPayload(filePath) {
  const baseName = path.basename(filePath);
  const title = titleCase(
    baseName.replace(path.extname(baseName), '').replace(/[_-]+/g, ' ')
  );

  if (!mammoth) {
    return {
      title,
      description: `Seeded from ${baseName}`,
      body: `See ${baseName} in the Seed doc materials directory for full content. Install mammoth to ingest docx text.`
    };
  }

  try {
    const result = await mammoth.extractRawText({ path: filePath });
    const normalized = normalizeWhitespace(result.value || '');
    const body = truncate(normalized, 8000);
    const description = truncate(normalized || `Seeded from ${baseName}`, 220);
    return { title, description, body };
  } catch (error) {
    console.warn(`[seed][material] Failed to parse ${baseName}: ${error.message}`);
    return {
      title,
      description: `Seeded from ${baseName}`,
      body: `Unable to parse ${baseName}; attach the file manually if needed.`
    };
  }
}

async function seedMaterials(db, teacherId, topics) {
  if (!existsSync(SEED_DOC_DIR)) {
    console.warn(`[seed][material] Directory missing: ${SEED_DOC_DIR}`);
    return { created: 0, updated: 0 };
  }

  const entries = await fs.readdir(SEED_DOC_DIR, { withFileTypes: true });
  const docFiles = entries
    .filter((entry) => entry.isFile() && entry.name.toLowerCase().endsWith('.docx'))
    .slice(0, MATERIAL_LIMIT);

  if (docFiles.length === 0) {
    console.warn('[seed][material] No .docx files found; skipping material creation');
    return { created: 0, updated: 0 };
  }

  let created = 0;
  let updated = 0;
  for (const [index, file] of docFiles.entries()) {
    const topic = topics[index % topics.length];
    const fullPath = path.join(SEED_DOC_DIR, file.name);
    const payload = await extractDocPayload(fullPath);
    const id = `seed-${slugify(file.name)}`;
    const ref = db.collection('materials').doc(id);
    const existing = await ref.get();
    const createdAt = existing.exists && typeof existing.get('createdAt') === 'number'
      ? existing.get('createdAt')
      : Date.now();
    await ref.set(
      {
        teacherId,
        classroomId: classroomConfig.id,
        classroomName: classroomConfig.name,
        topicId: topic.id,
        topicName: topic.name,
        title: payload.title,
        description: payload.description,
        body: payload.body,
        createdAt,
        updatedAt: Date.now(),
        isArchived: false,
        archived: false,
        archivedAt: null
      },
      { merge: true }
    );
    if (existing.exists) {
      updated += 1;
    } else {
      created += 1;
    }
  }
  return { created, updated };
}

function buildInterestQuestions(topicName) {
  const questions = [
    {
      stem: 'Which formula computes simple interest?',
      choices: ['I = PRT', 'A = P(1 + r/n)^(nt)', 'FV = PV / (1 + r)^t', 'I = P(1 + rt)'],
      correct: ['I = PRT']
    },
    {
      stem: 'If interest is added back to the principal each period, this is:',
      choices: ['Simple interest', 'Compound interest', 'Discounting', 'Amortization'],
      correct: ['Compound interest']
    },
    {
      stem: 'A loan of $10,000 at 6% simple interest for 3 years earns how much interest?',
      choices: ['$600', '$1,800', '$2,400', '$3,600'],
      correct: ['$1,800']
    },
    {
      stem: 'At 8% compounded annually, which amount is closest to the future value of $5,000 after 5 years?',
      choices: ['$6,000', '$7,346', '$7,999', '$8,250'],
      correct: ['$7,346']
    },
    {
      stem: 'True or False: More compounding periods per year generally increases future value (holding rate and time constant).',
      choices: ['True', 'False'],
      correct: ['True']
    },
    {
      stem: 'Which variable is the “time” term in simple interest?',
      choices: ['Number of compounding periods per year', 'Total number of years', 'Annual percentage rate', 'Principal'],
      correct: ['Total number of years']
    },
    {
      stem: 'Distinguish: Simple interest uses a constant base; compound interest uses:',
      choices: ['A decreasing base', 'An increasing base', 'No base', 'A negative base'],
      correct: ['An increasing base']
    },
    {
      stem: 'Which is larger after 4 years on $2,000 at 9%: simple or annual compounding?',
      choices: ['Simple interest', 'Compound interest', 'They are equal', 'Cannot be determined'],
      correct: ['Compound interest']
    },
    {
      stem: 'Future value of $1,500 at 5% simple interest after 6 years?',
      choices: ['$1,950', '$1,995', '$2,000', '$2,250'],
      correct: ['$1,950']
    },
    {
      stem: 'Present value is best described as:',
      choices: [
        'The value of a future sum discounted back to today',
        'The interest earned each year',
        'The maturity value of a loan',
        'The coupon rate of a bond'
      ],
      correct: ['The value of a future sum discounted back to today']
    },
    {
      stem: 'If the nominal rate is 12% with monthly compounding, the periodic rate is:',
      choices: ['12%', '1%', '0.12%', 'Cannot be known'],
      correct: ['1%']
    },
    {
      stem: 'Which is the maturity value for simple interest?',
      choices: ['Principal only', 'Principal + interest', 'Interest only', 'Principal – interest'],
      correct: ['Principal + interest']
    },
    {
      stem: 'True or False: Discounting uses the same rate and time concepts as compounding but in reverse.',
      choices: ['True', 'False'],
      correct: ['True']
    },
    {
      stem: 'A deposit grows from $2,500 to $3,150 in one year. The implied simple interest rate is closest to:',
      choices: ['12%', '20%', '26%', '32%'],
      correct: ['26%']
    },
    {
      stem: 'Which scenario best matches simple interest?',
      choices: [
        'Credit card debt adding interest monthly to balance',
        'Bank savings account with quarterly compounding',
        'A short-term note that pays interest only on principal',
        'Dividend reinvestment plan'
      ],
      correct: ['A short-term note that pays interest only on principal']
    }
  ];

  return questions.map((q, index) => ({
    id: `q${index + 1}`,
    quizId: '',
    type: index === 4 || index === 13 ? 'tf' : 'mcq',
    stem: `${topicName}: ${q.stem}`,
    choices: q.choices,
    answerKey: q.correct,
    explanation: '',
    timeLimitSeconds: 60
  }));
}

async function upsertTopics(db, teacherId, topics) {
  const createdTopics = [];
  for (const topic of topics) {
    const ref = db.collection('topics').doc(topic.id);
    const existing = await ref.get();
    const createdAt = existing.exists && typeof existing.get('createdAt') === 'number'
      ? existing.get('createdAt')
      : Date.now();
    await ref.set(
      {
        classroomId: classroomConfig.id,
        teacherId,
        name: topic.name,
        description: topic.description,
        createdAt,
        updatedAt: Date.now(),
        isArchived: false,
        archived: false,
        archivedAt: null
      },
      { merge: true }
    );
    createdTopics.push(topic);
  }
  return createdTopics;
}

async function seedQuizzesAndAssignments(db, teacherId, topics) {
  const now = Date.now();
  const closeAt = now + 14 * 24 * 60 * 60 * 1000; // 14 days
  let quizzesCreated = 0;
  let assignmentsCreated = 0;

  for (const topic of topics) {
    const baseQuestions = buildInterestQuestions(topic.name);
    const quizDefs = [
      { kind: 'pre', title: `${topic.name}: Pre-Test` },
      { kind: 'post', title: `${topic.name}: Post-Test` }
    ];

    for (const quizDef of quizDefs) {
      const quizId = `seed-quiz-${topic.id}-${quizDef.kind}`;
      const questions = baseQuestions.map((q) => ({
        ...q,
        quizId,
        id: `${quizId}-${q.id}`
      }));
      const quizRef = db.collection('quizzes').doc(quizId);
      await quizRef.set(
        {
          teacherId,
          classroomId: classroomConfig.id,
          topicId: topic.id,
          title: quizDef.title,
          defaultTimePerQ: 60,
          shuffle: true,
          createdAt: now,
          updatedAt: now,
          questionCount: questions.length,
          questionsJson: JSON.stringify(questions),
          category: 'STANDARD',
          isArchived: false,
          archivedAt: null
        },
        { merge: true }
      );
      quizzesCreated += 1;

      const assignmentId = `seed-assignment-${topic.id}-${quizDef.kind}`;
      const assignmentRef = db.collection('assignments').doc(assignmentId);
      await assignmentRef.set(
        {
          quizId,
          classroomId: classroomConfig.id,
          topicId: topic.id,
          openAt: now,
          closeAt,
          attemptsAllowed: 3,
          scoringMode: 'BEST',
          revealAfterSubmit: true,
          createdAt: now,
          updatedAt: now,
          isArchived: false,
          archived: false,
          archivedAt: null
        },
        { merge: true }
      );
      assignmentsCreated += 1;
    }
  }

  return { quizzesCreated, assignmentsCreated };
}

async function main() {
  if (admin.apps.length === 0) {
    admin.initializeApp();
  }
  const auth = admin.auth();
  const db = admin.firestore();
  db.settings({ ignoreUndefinedProperties: true });

  console.log('[seed] Starting demo seed...');
  const teacher = await ensurePasswordUser(auth, teacherConfig, 'teacher');
  const students = [];
  for (const student of studentConfig) {
    students.push(await ensurePasswordUser(auth, student, 'student'));
  }

  await upsertTeacherProfile(db, teacher);
  await Promise.all(students.map((student) => upsertStudentProfile(db, student)));
  await upsertClassroom(
    db,
    teacher.uid,
    students.map((student) => student.uid)
  );
  const topics = await upsertTopics(db, teacher.uid, buildTopicConfigs());
  const quizResult = await seedQuizzesAndAssignments(db, teacher.uid, topics);
  const materialResult = await seedMaterials(db, teacher.uid, topics);

  console.log('[seed] Done.');
  console.log(`- Teacher: ${teacher.email} (${teacher.uid})`);
  console.log(`- Students (${students.length}): ${students.map((s) => s.email).join(', ')}`);
  console.log(`- Classroom: ${classroomConfig.name} [${classroomConfig.id}] join code ${classroomConfig.joinCode}`);
  console.log(`- Topics: ${topics.length} created/updated`);
  console.log(`- Quizzes: ${quizResult.quizzesCreated} created (pre/post per topic)`);
  console.log(`- Assignments: ${quizResult.assignmentsCreated} created (attempts 3, scoring BEST)`);
  console.log(`- Materials: ${materialResult.created} created, ${materialResult.updated} updated (source: ${SEED_DOC_DIR})`);
  console.log(`- Default password: "${DEFAULT_PASSWORD}"${SHOULD_RESET_PASSWORDS ? ' (reset applied)' : ''}`);
}

if (require.main === module) {
  main().catch((error) => {
    console.error('[seed] Failed', error);
    process.exit(1);
  });
}
