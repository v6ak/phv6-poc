# *P*assword *h*ashing by *v6*ak (PHv6)

Current status: **rather a dump of my instant ideas**

**Even if you are able to compile the code, you are strongly advised not to use this code in production systems at the moment. Neither the principle itself nor its implementation has been reviewed enough.**

## Motivation
When you use scrypt, you have to store the cost parameters. It may be increased over the time. This has, however, some downsides. When attacked gets the DB dump, the cost parameter contains some information. Most importantly, it is an important parameter for brute force attack.

## Main difference from scrypt: the cost parameter(s)
PHv6 does not require cost parameter to be stored for each password separately. If you use different cost value, you can't verify. With PHv6, if you specify higher cost for verification, you can still verify the password. If you specify a lower cost for verification, you will fail to verify it. You should have no idea if you have failed the verification just because of a bad password or because of low cost parameter.

It is however possible for a known password and hash to determine the cost used. This can't be effectively mitigated, I think. Even if we tried hard to do so, attacker can bruteforce (maybe using a bisection) the cost parameter.

If there is no known password (for example in a system with closed registration), attacker needs to use a cost with a reasonable margin. If there is an old known password, it is likely that more recent passwords. It there is a fresh known password, attacker might assume that this is the maximum cost.

Unlike scrypt, attacker can never simply find passwords with low cost parameter.

## Basic principles
The algorithm uses a hash function and an encryption function. The hash function ensures constant length of the output. The encryption function is used for increasing the cost. While it might sound weird to use encryption for these purposes, it is reasonable.

*TODO:* Think again if this can be achieved only using hash functions without encryption and compare pros/cons.

## Hash age leakage

With some usages, the attacker with a DB dump might get the hash age from another column like `last_login`. He might then do some assumptions (and maybe some measurements) on the cost needed. It might be a good idea to encrypt such information using (directly or indirectly) the password itself.

## Higher cost for weak passwords?

It might sound like a great idea to choose higher cost for weak passwords. (Thanks Jakub Vrána for this idea. [1]) It has, however, some important downsides. They depend on the variant used.

First, assume that we have a constant-time computation. That is, no matter the actual cost is, the password verification will take about the same. (Well, it may slightly depend on the password size because of the initial hashing, but this should not be significant.) In this case, we could use the higher cost for all passwords. This results in the same load on our server. Even strong passwords would be hashed with a large cost. The only advantage provided by the discrimination between strong and weak passwords seems to be some uncertainty about the cost in a known-passwords scenarios.

Second, assume we have a computation that takes lower time with a lower actual cost. This allows some timing attacks even on encrypted connection. For example, when it takes longer to log in for a particular user, it might indicate a weak password. Once a username of the user is known (maybe guessed or assumed to be the same as on a different website that uses non-encrypted login), the attacker is more likely to try to attack this user's password.

## Partially random cost?

Because the attacker is able to compute the cost of a hash with a known password, it might be a good idea to use a random password cost from a reasonable interval. However, if the attacker knows multiple passwords with almost the same freshness, he may compute the distribution and guess the maximum freshness. It may be discussed how much the random cost affects the security in various scenarios.

## Other cost factors
The scrypt algorithm has multiple cost parameters. I've considered one more parameter: parallelization degree. Since offline brute-force attacks are embarrassingly parallel, allowing him to parallelize the computation brings him nothing. Allowing the legitimate user to parallelize the computation may, however, make it faster for legitimate users, which may make higher costs more affordable.

However, it might bring no advantage in a scenario of a busy server.

I've tried to consider traditional parallelization, but it seems that synchronization overhead is too high. I've considered (but not implemented) a parallelization wrapper. It basically means there are multiple hashes, one of them is correct. The password is accepted iff it is accepted by serial version of this function on one of the passwords.

## Collision resistance
The collision resistance depends on the used hash function and the used encryption function. However, higher cost will increase the collision probability. It will, however, also increase a computation time. It can be shown that cost has no impact or almost no impact on ability of the attacker to find such vulnerability.

## ASIC resistance
In order to be resistant to ASIC/FPGA/GPU, it seems to be a good idea to use an encryption algorithm that does not perform well on these machines. We might try to make a stream cipher from scrypt with low cost, but this idea should be very strongly reviewed.

## Future ideas
There ideas are rather related to the library, not so much to the algorithm itself.
* updating cost
* salts

---

[1] http://php.vrana.cz/tajemstvi.php